package com.uc.caffeine.data

import com.uc.caffeine.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

/** What Open Food Facts knows about a scanned product, cut down to what a custom drink needs. */
data class ScannedProduct(
    val name: String,
    val categoryTags: List<String>,
    /** Per 100 ml, or per 100 g for solids; null when the product has no caffeine figure. */
    val caffeineMgPer100: Double?,
    val packageAmount: Double?,
    /** "ml" or "g", when known. */
    val packageUnit: String?,
)

sealed interface ProductLookup {
    data class Found(val product: ScannedProduct) : ProductLookup
    data object NotFound : ProductLookup
    data object Unreachable : ProductLookup
}

/**
 * Barcode lookups against Open Food Facts, the free and open product database. Only made after the
 * user agrees for that scan, and only the barcode number is sent, with the User-Agent the service
 * asks apps to identify themselves by. Its data is ODbL-licensed, so the UI credits it.
 */
object OpenFoodFacts {

    // "quantity" has to be asked for: with "nutriments" requested and it left out, the API drops
    // product_quantity and product_quantity_unit from the response.
    private const val FIELDS =
        "product_name,generic_name,brands,categories_tags,nutriments,quantity,product_quantity," +
            "product_quantity_unit,ingredients_text"
    private const val TIMEOUT_MS = 10_000

    suspend fun lookUp(barcode: String): ProductLookup = withContext(Dispatchers.IO) {
        require(barcode.isNotEmpty() && barcode.all { it.isDigit() }) { "Not a product barcode: $barcode" }
        val connection = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=$FIELDS")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty(
                "User-Agent",
                "CaffeineHealth/${BuildConfig.VERSION_NAME} (Android; https://github.com/ohuc/CaffeineHealth)",
            )
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                HttpURLConnection.HTTP_NOT_FOUND -> ProductLookup.NotFound
                else -> ProductLookup.Unreachable
            }
        } catch (_: IOException) {
            ProductLookup.Unreachable
        } catch (_: JSONException) {
            ProductLookup.Unreachable
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): ProductLookup {
        val root = JSONObject(body)
        val product = root.optJSONObject("product")
        if (root.optInt("status") != 1 || product == null) return ProductLookup.NotFound

        val name = product.optString("product_name").ifBlank { product.optString("generic_name") }.trim()
        val brand = product.optString("brands").substringBefore(',').trim()
        val tags = product.optJSONArray("categories_tags")
            ?.let { array -> List(array.length()) { array.optString(it) } }
            .orEmpty()
        // Open Food Facts normalises *_100g nutrients to grams.
        val caffeineGrams = product.optJSONObject("nutriments")?.optDouble("caffeine_100g")
        // The label's own wording gives one item of a multipack; product_quantity is the whole pack.
        val labelSize = parsePackageSize(product.optString("quantity"))
        val amount = product.optDouble("product_quantity")
        return ProductLookup.Found(
            ScannedProduct(
                name = when {
                    name.isBlank() -> brand
                    brand.isBlank() || name.contains(brand, ignoreCase = true) -> name
                    else -> "$brand $name"
                },
                categoryTags = tags,
                caffeineMgPer100 = caffeineGrams?.takeIf { it > 0.0 }?.times(1000)
                    ?: caffeineFromIngredients(product.optString("ingredients_text")),
                packageAmount = labelSize?.first ?: amount.takeIf { it > 0.0 },
                packageUnit = labelSize?.second
                    ?: product.optString("product_quantity_unit").lowercase().takeIf { it == "ml" || it == "g" },
            )
        )
    }
}

private const val ML_PER_FL_OZ = 29.5735

private val PACKAGE_SIZE = Regex("""(\d+(?:[.,]\d+)?)\s*(ml|cl|l|fl\.?\s*oz|kg|g)\b""")

/**
 * One item's size from a label quantity like "250 ml", "33 cl", "1,5 L" or "8.4 fl oz", in ml or g.
 * "4 x 250 ml" reads as one 250 ml can, and a metric figure wins over fluid ounces.
 */
internal fun parsePackageSize(quantity: String): Pair<Double, String>? =
    PACKAGE_SIZE.findAll(quantity.lowercase())
        .sortedBy { it.groupValues[2].startsWith("fl") }
        .firstNotNullOfOrNull { match ->
            val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                ?: return@firstNotNullOfOrNull null
            when (match.groupValues[2].filter(Char::isLetter)) {
                "ml" -> amount to "ml"
                "cl" -> amount * 10 to "ml"
                "l" -> amount * 1000 to "ml"
                "floz" -> amount * ML_PER_FL_OZ to "ml"
                "g" -> amount to "g"
                "kg" -> amount * 1000 to "g"
                else -> null
            }
        }

private val INGREDIENT_CAFFEINE = Regex("""(?:caff?[eé][iíï]n[ae]?|koff?e[iï]n[ae]?)\W{0,3}(\d+(?:[.,]\d+)?)\s*%""")

/**
 * Caffeine in mg per 100 g as an ingredients list states it, like "Caffeine (0.03%)". Plenty of
 * products carry that line without the caffeine nutrient filled in.
 */
internal fun caffeineFromIngredients(ingredients: String): Double? =
    INGREDIENT_CAFFEINE.find(ingredients.lowercase())
        ?.groupValues?.get(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        // Anything past a few percent is a misread, not a drink.
        ?.takeIf { it > 0.0 && it <= 5.0 }
        ?.times(1000)

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val NAME_PACK_SIZE = Regex("""\b\d+(?:[.,]\d+)?\s*(?:ml|cl|l|fl\.?\s*oz|oz|kg|g)\b""")
private val NAME_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
private val FILLER_WORDS = setOf(
    "the", "energy", "drink", "drinks", "beverage", "original", "classic", "can", "bottle",
    "energydrink", "energiedrink", "getrank", "boisson", "energisante", "bebida", "energetica", "energetico",
)

/**
 * A drink name boiled down so a looked-up product can be matched to a drink already in the app:
 * accents, punctuation, pack sizes and filler like "energy drink" go, so "Red Bull Energy Drink
 * 250 ml" finds Red Bull, while "Red Bull Sugar Free" finds only Red Bull Sugarfree. Empty when
 * nothing distinctive is left, which matches nothing.
 */
internal fun drinkMatchKey(name: String): String =
    Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace(NAME_PACK_SIZE, " ")
        .split(NAME_SEPARATORS)
        .filter { it.isNotEmpty() && it !in FILLER_WORDS }
        .joinToString("")

/** Starting values for the custom drink sheet. */
data class CustomDrinkDraft(
    val name: String = "",
    val category: String = "coffee",
    val unitKey: String = "cup",
    val caffeineMg: Double? = null,
)

/**
 * A looked-up product as custom drink values. A liquid with a known size becomes one container (a
 * can for energy drinks and sodas up to 500 ml, a bottle otherwise) carrying the whole pack's
 * caffeine; otherwise it is logged by the millilitre or gram.
 */
fun ScannedProduct.toCustomDrinkDraft(): CustomDrinkDraft {
    val category = appCategoryFor(categoryTags)
    val perUnit = caffeineMgPer100?.div(100.0)
    // Without a size on the label the caffeine figure still holds per millilitre or per gram.
    val unit = packageUnit ?: perUnit?.let { if ("en:beverages" in categoryTags) "ml" else "g" }
    return when {
        unit == "ml" && packageAmount != null -> CustomDrinkDraft(
            name = name,
            category = category,
            unitKey = if (packageAmount <= 500.0 && (category == "energy_drink" || category == "soft_drink")) "can" else "bottle",
            caffeineMg = perUnit?.times(packageAmount),
        )
        unit != null -> CustomDrinkDraft(name, category, unit, perUnit)
        else -> CustomDrinkDraft(name = name, category = category)
    }
}

/** Maps Open Food Facts category tags (like "en:energy-drinks") onto the app's categories. */
private fun appCategoryFor(tags: List<String>): String {
    val words = tags.map { it.substringAfter(':').split('-').toSet() }
    fun any(vararg candidates: String) = words.any { tagWords -> candidates.any { it in tagWords } }
    return when {
        any("energy") -> "energy_drink"
        any("coffee", "coffees", "espresso") -> "coffee"
        any("tea", "teas", "mate", "matcha") -> "tea"
        any("cola", "colas", "soda", "sodas", "soft", "carbonated") -> "soft_drink"
        any("chocolate", "chocolates", "cocoa") -> "chocolate"
        else -> "coffee"
    }
}

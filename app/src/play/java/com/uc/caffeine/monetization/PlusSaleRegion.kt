package com.uc.caffeine.monetization

/** Whether Caffeine Health+ can be sold to this user's Google Play country. */
internal enum class SaleRegion { UNKNOWN, OPEN, CLOSED }

/**
 * Where Caffeine Health+ is on sale.
 *
 * Indian developers must finish BillDesk's PA-CB KYC before Google Play sells to buyers outside
 * India, so Plus can start in a limited set of countries. Outside them the Play edition behaves
 * like the free app: no locks, no Plus prompts, no checkout. Locking features a user cannot buy
 * would be worse than giving them away until the sale opens.
 *
 * The country list is a comma-separated set of ISO 3166-1 alpha-2 codes, or `*` for everywhere.
 * It comes from the RevenueCat current offering's metadata key [METADATA_KEY] when set (a
 * dashboard change, no app update), else from the build default `PLUS_SALE_COUNTRIES`.
 */
internal object PlusSaleRegion {
    const val METADATA_KEY = "plus_sale_countries"
    const val EVERYWHERE = "*"

    /** The remote list wins when it says anything; a blank or missing value falls back to the build. */
    fun effectiveSpec(remote: String?, buildDefault: String): String =
        remote?.takeIf { it.isNotBlank() } ?: buildDefault

    /**
     * A blank list means no restriction (the behaviour before regions existed). An unknown or
     * malformed store country is [SaleRegion.UNKNOWN], which callers treat as not on sale.
     */
    fun resolve(spec: String, storefrontCountry: String?): SaleRegion {
        val countries = spec.split(',', ';', ' ').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        if (countries.isEmpty() || EVERYWHERE in countries) return SaleRegion.OPEN
        val country = storefrontCountry?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all(Char::isLetter) }
            ?: return SaleRegion.UNKNOWN
        return if (country in countries) SaleRegion.OPEN else SaleRegion.CLOSED
    }
}

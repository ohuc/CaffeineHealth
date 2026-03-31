package com.uc.caffeine.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utility object for mapping drink category values from JSON to user-friendly display names
 * and managing category ordering for consistent display across the app.
 */
object CategoryUtils {

    /**
     * Map of JSON category values to their display names
     */
    private val categoryDisplayNames = mapOf(
        "coffee" to "Coffee",
        "energy_drink" to "Energy Drinks",
        "soft_drink" to "Soft Drinks",
        "tea" to "Tea",
        "chocolate" to "Chocolate",
        "pill" to "Pills"
    )

    /**
     * Ordered list of category keys for consistent display ordering
     * Order: Coffee, Energy Drinks, Tea, Soft Drinks, Chocolate, Pills
     */
    private val categoryOrder = listOf(
        "coffee",
        "energy_drink",
        "tea",
        "soft_drink",
        "chocolate",
        "pill"
    )

    /**
     * Get the user-friendly display name for a category
     * @param category The category value from JSON (e.g., "coffee", "energy_drink")
     * @return The display name (e.g., "Coffee", "Energy Drinks"), or the original category if not found
     */
    fun getCategoryDisplayName(category: String): String {
        return categoryDisplayNames[category] ?: category.replaceFirstChar { it.uppercase() }
    }

    /**
     * Get the ordered list of category keys for consistent display
     * @return List of category keys in display order
     */
    fun getCategoryOrder(): List<String> {
        return categoryOrder
    }

    /**
     * Get the ordered list of category display names
     * @return List of category display names in display order
     */
    fun getCategoryDisplayNamesOrdered(): List<String> {
        return categoryOrder.mapNotNull { categoryDisplayNames[it] }
    }

    /**
     * Get all category mappings
     * @return Map of category keys to display names
     */
    fun getAllCategories(): Map<String, String> {
        return categoryDisplayNames
    }

    /**
     * Get the sort order index for a category
     * @param category The category value from JSON
     * @return The sort index (0-based), or Int.MAX_VALUE if category not found
     */
    fun getCategorySortOrder(category: String): Int {
        val index = categoryOrder.indexOf(category)
        return if (index >= 0) index else Int.MAX_VALUE
    }

    /**
     * Sort a list of items by category order
     * @param items List of items to sort
     * @param categorySelector Function to extract category from an item
     * @return Sorted list
     */
    fun <T> sortByCategory(items: List<T>, categorySelector: (T) -> String): List<T> {
        return items.sortedBy { getCategorySortOrder(categorySelector(it)) }
    }

    /**
     * Get abbreviated label for segmented button display
     * Returns shorter versions of long category names to prevent overflow
     * @param displayName The full category display name (e.g., "Energy Drinks")
     * @return The abbreviated label suitable for button display (e.g., "Energy")
     */
    fun getCategoryButtonLabel(displayName: String): String {
        return when (displayName) {
            "Energy Drinks" -> "Energy"
            "Soft Drinks" -> "Soft Drink"
            else -> displayName
        }
    }
}

/**
 * Object for mapping category display names to Material Icons
 */
object CategoryIcons {
    /**
     * Get the icon for a category display name
     * @param categoryDisplayName The display name (e.g., "Coffee", "Energy Drinks")
     * @return The corresponding Material Icon
     */
    fun getIcon(categoryDisplayName: String): ImageVector {
        return when (categoryDisplayName) {
            "Coffee" -> Icons.Filled.LocalCafe
            "Energy Drinks" -> Icons.Filled.BatteryChargingFull
            "Tea" -> Icons.Filled.EmojiFoodBeverage
            "Soft Drinks" -> Icons.Filled.LocalDrink
            "Chocolate" -> Icons.Filled.Cookie
            "Pills" -> Icons.Filled.Medication
            else -> Icons.Filled.Category
        }
    }
    
    /**
     * Get the icon for the "All" filter option
     * @return The GridView icon
     */
    fun getAllIcon(): ImageVector {
        return Icons.Filled.GridView
    }
}

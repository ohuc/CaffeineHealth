package com.uc.caffeine

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import java.io.Serializable

sealed interface AppRoute : NavKey

enum class AppDestinations(
    @param:StringRes val labelRes: Int,
    val iconOutlinedRes: Int? = null,
    val iconFilledRes: Int? = null,
    val iconOutlinedVector: ImageVector? = null,
    val iconFilledVector: ImageVector? = null,
) : AppRoute {
    HOME(
        labelRes = R.string.nav_home,
        iconOutlinedRes = R.drawable.ic_home,
        iconFilledRes = R.drawable.ic_home_filled,
    ),
    ANALYTICS(
        labelRes = R.string.nav_analytics,
        iconOutlinedVector = Icons.Outlined.Analytics,
        iconFilledVector = Icons.Filled.Analytics,
    ),
    SETTINGS(
        labelRes = R.string.nav_settings,
        iconOutlinedVector = Icons.Outlined.Settings,
        iconFilledVector = Icons.Filled.Settings,
    ),
}

object AddRoute : AppRoute, Serializable

internal val toolbarDestinations = listOf(
    AppDestinations.HOME,
    AppDestinations.ANALYTICS,
    AppDestinations.SETTINGS,
)

internal fun AppRoute?.resolveToolbarDestination(): AppDestinations {
    return when (this) {
        null,
        AddRoute,
        AppDestinations.HOME -> AppDestinations.HOME

        AppDestinations.ANALYTICS -> AppDestinations.ANALYTICS
        AppDestinations.SETTINGS -> AppDestinations.SETTINGS
    }
}

internal fun AppRoute?.shouldShowHomeFab(): Boolean = this == AppDestinations.HOME

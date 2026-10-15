package com.uc.caffeine.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.currentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Per-instance Glance state shared by every Caffeine widget. */
internal object WidgetStateKeys {
    /**
     * Bumped by [CaffeineWidgetUpdater.update]. While a widget's Glance session is alive, `update()`
     * only re-reads this state and recomposes — it never re-runs [GlanceAppWidget.provideGlance] —
     * so the composition reloads its data whenever the token changes.
     */
    val RefreshToken = longPreferencesKey("refresh_token")

    /** [WidgetDrink.key] of the drink just logged from this instance, for the tile confirmation. */
    val LoggedDrinkKey = stringPreferencesKey("logged_drink_key")
    val LoggedAtMillis = longPreferencesKey("logged_at_millis")

    /** How long a tile shows "Logged" and ignores repeat taps on the same drink. */
    const val CONFIRMATION_MS = 1_800L
}

/**
 * Shared plumbing for the Caffeine widgets: data loading that stays live across a running session,
 * theming, and generated previews. Subclasses only lay out [Content].
 *
 * [SizeMode.Exact] because every layout adapts to its real size (tile counts, chart bitmap pixels)
 * rather than snapping to a few breakpoints.
 *
 * Subclasses also override [previewSizeMode]. Glance otherwise composes the picker preview once, at
 * the widget's minimum size, and the picker then lays that out at the target cell size: fixed-width
 * content renders as a sliver and size-dependent layouts drop their tiles. With a few responsive
 * sizes, the picker applies whichever is closest to the space it actually has.
 */
abstract class CaffeineGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetDataRepository(context)
        val initialToken = getAppWidgetState<Preferences>(context, id)[WidgetStateKeys.RefreshToken]
        val initial = withContext(Dispatchers.IO) { repository.load() }
        provideContent {
            val token = currentState(WidgetStateKeys.RefreshToken)
            val data by produceState(initial, token) {
                if (token != initialToken) value = withContext(Dispatchers.IO) { repository.load() }
            }
            val loggedKey = currentState(WidgetStateKeys.LoggedDrinkKey)
            val loggedAt = currentState(WidgetStateKeys.LoggedAtMillis) ?: 0L
            // A process death between logging and clearing must not pin the confirmation forever.
            val confirmedKey = loggedKey?.takeIf {
                System.currentTimeMillis() - loggedAt < WidgetStateKeys.CONFIRMATION_MS
            }
            WidgetTheme(colorPalette = data.settings.colorPalette) {
                Content(data = data, confirmedDrinkKey = confirmedKey, isPreview = false)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetDataRepository(context).mockData()
        provideContent {
            WidgetTheme(colorPalette = data.settings.colorPalette) {
                Content(data = data, confirmedDrinkKey = null, isPreview = true)
            }
        }
    }

    @Composable
    internal abstract fun Content(data: WidgetData, confirmedDrinkKey: String?, isPreview: Boolean)
}

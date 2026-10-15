package com.uc.caffeine.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.uc.caffeine.MainActivity
import com.uc.caffeine.R
import com.uc.caffeine.util.formatServingSummary
import kotlin.math.floor

/** Gap between tiles, and between a tile group and the widget edge. */
internal val TileGap = 6.dp

/** Root container: system corner radius, launcher-recognised background, opens the app on tap. */
@Composable
internal fun WidgetSurface(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(widgetPalette.background)
            .clickable(actionStartActivity<MainActivity>()),
        content = content,
    )
}

@Composable
internal fun LevelHeader(showRefresh: Boolean) {
    val context = LocalContext.current
    val palette = widgetPalette
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetLogo()
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = context.getString(R.string.widget_caffeine_now_inline_label),
            style = TextStyle(color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (showRefresh) RefreshButton()
    }
}

/** The app icon in its round launcher form; untinted so it reads as the brand mark on any palette. */
@Composable
internal fun WidgetLogo() {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_logo),
        contentDescription = null,
        modifier = GlanceModifier.size(20.dp),
    )
}

@Composable
private fun RefreshButton() {
    val context = LocalContext.current
    val palette = widgetPalette
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .cornerRadius(14.dp)
            .background(palette.tile)
            .clickable(actionRunCallback<RefreshWidgetAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getString(R.string.widget_refresh_cd),
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(palette.textMuted),
        )
    }
}

@Composable
internal fun LevelReadout(data: WidgetData, fontSize: TextUnit) {
    val context = LocalContext.current
    val value = data.currentCaffeineMg.toWidgetNumberString()
    ReadoutText(
        value = value,
        suffix = context.getString(R.string.unit_mg),
        fontSize = fontSize,
        color = widgetPalette.accent,
        contentDescription = context.getString(R.string.widget_level_cd, value),
    )
}

/** Projected level at bedtime, colored with the same bands as the Home sleep forecast. */
@Composable
internal fun BedtimeLine(data: WidgetData) {
    val context = LocalContext.current
    val palette = widgetPalette
    val (tint, text) = when (data.sleepOutlook) {
        SleepOutlook.Clear ->
            palette.accent to context.getString(R.string.sleep_forecast_safe, data.bedtimeLabel)
        SleepOutlook.Caution -> palette.warning to context.getString(
            R.string.widget_bedtime_projection, data.caffeineAtBedtimeMg.toInt(), data.bedtimeLabel,
        )
        SleepOutlook.Disruptive -> palette.danger to context.getString(
            R.string.widget_bedtime_projection, data.caffeineAtBedtimeMg.toInt(), data.bedtimeLabel,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_moon),
            contentDescription = null,
            modifier = GlanceModifier.size(13.dp),
            colorFilter = ColorFilter.tint(tint),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = text,
            style = TextStyle(color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/**
 * The decay curve, bottom-aligned and full-bleed across the parent's width, with a hairline at "now".
 * [width] only sizes the bitmap: the image stretches to the width the host actually lays out, which
 * differs from the composed size in picker previews. Renders nothing when there is no caffeine in
 * the 12-hour window.
 */
@Composable
internal fun ChartLayer(data: WidgetData, width: Dp, height: Dp) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    // Capped so a stretched tablet widget doesn't ship a multi-megabyte bitmap per update.
    val widthPx = (width.value * density).toInt().coerceIn(1, 1200)
    val heightPx = (height.value * density).toInt().coerceIn(1, 600)
    val bitmap = remember(data.allEntries, data.bedtimeMillis, data.settings, widthPx, heightPx) {
        WidgetChartRenderer.render(
            entries = data.allEntries,
            settings = data.settings,
            widthPx = widthPx,
            heightPx = heightPx,
            bedtimeMillis = data.bedtimeMillis,
        )
    } ?: return
    val palette = widgetPalette
    Box(
        modifier = GlanceModifier.fillMaxWidth().height(height),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(palette.accent),
        )
        // The chart window is symmetric around now, so equal weights keep the hairline on it at
        // any laid-out width.
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(palette.divider),
            ) {}
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Quick-log tiles
// ---------------------------------------------------------------------------------------------

/** Tile treatments, picked from the space a tile actually gets (see [tileStyleFor]). */
internal enum class TileStyle {
    /** Icon over name and mg, with a + badge. Needs a roughly square tile. */
    Stacked,

    /** Icon over mg only, for short tiles. */
    Compact,

    /** Icon beside name and mg, for short but wide tiles. */
    Inline,

    /** A full-width row: icon, name, serving and mg, and a + button. */
    Pill,
}

internal fun tileStyleFor(width: Dp, height: Dp): TileStyle = when {
    width >= 180.dp && height >= 44.dp && height < 96.dp -> TileStyle.Pill
    height >= 84.dp && width >= 84.dp -> TileStyle.Stacked
    width >= 116.dp && height >= 40.dp -> TileStyle.Inline
    else -> TileStyle.Compact
}

/** How many tiles of at least [minTile] fit in [available], capped to [max]. */
internal fun tilesThatFit(available: Dp, minTile: Dp, max: Int): Int {
    if (max <= 0) return 0
    val fit = floor(((available + TileGap) / (minTile + TileGap)).toDouble()).toInt()
    return fit.coerceIn(0, max)
}

/**
 * A recent drink that logs itself on tap. While [confirmed], the tile flips to the accent color
 * with a check so the tap visibly landed (the log action also ignores repeat taps meanwhile).
 */
@Composable
internal fun DrinkTile(
    drink: WidgetDrink,
    style: TileStyle,
    confirmed: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val palette = widgetPalette
    val info = drink.info
    val container = if (confirmed) palette.accent else palette.tile
    val primaryText = if (confirmed) palette.onAccent else palette.text
    val secondaryText = if (confirmed) palette.onAccent else palette.textMuted
    val name = if (confirmed) context.getString(R.string.widget_logged) else info.drinkName
    val mg = "${info.caffeineMg} ${context.getString(R.string.unit_mg)}"
    val description = context.getString(R.string.widget_log_drink_cd, info.drinkName, info.caffeineMg)

    val tileModifier = modifier
        .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
        .background(container)
        .semantics { contentDescription = description }
        .clickable(
            actionRunCallback<QuickLogAction>(
                actionParametersOf(
                    QuickLogAction.KEY_PRESET_ID to info.presetItemId,
                    QuickLogAction.KEY_DRINK_NAME to info.drinkName,
                    QuickLogAction.KEY_CAFFEINE_MG to info.caffeineMg,
                    QuickLogAction.KEY_QUANTITY to info.quantity,
                    QuickLogAction.KEY_UNIT_KEY to info.unitKey,
                    QuickLogAction.KEY_UNIT_CAFFEINE_MG to info.unitCaffeineMg,
                    QuickLogAction.KEY_EMOJI to info.emoji,
                    QuickLogAction.KEY_IMAGE_NAME to info.imageName,
                    QuickLogAction.KEY_ABSORPTION_RATE to info.absorptionRate,
                    QuickLogAction.KEY_DURATION_MINUTES to info.durationMinutes,
                    QuickLogAction.KEY_DRINK_KEY to drink.key,
                )
            )
        )

    when (style) {
        TileStyle.Stacked -> Column(modifier = tileModifier.padding(10.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                DrinkIcon(drink, size = 34.dp, confirmed = confirmed)
                Spacer(GlanceModifier.defaultWeight())
                if (!confirmed) AddBadge(size = 22.dp)
            }
            Spacer(GlanceModifier.defaultWeight())
            TileText(name, 13.sp, primaryText, FontWeight.Medium)
            TileText(mg, 11.sp, secondaryText, FontWeight.Normal)
        }

        TileStyle.Compact -> Column(
            modifier = tileModifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrinkIcon(drink, size = 30.dp, confirmed = confirmed)
            Spacer(GlanceModifier.height(4.dp))
            TileText(if (confirmed) name else mg, 11.sp, primaryText, FontWeight.Medium)
        }

        TileStyle.Inline -> Row(
            modifier = tileModifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrinkIcon(drink, size = 30.dp, confirmed = confirmed)
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                TileText(name, 12.sp, primaryText, FontWeight.Medium)
                TileText(mg, 11.sp, secondaryText, FontWeight.Normal)
            }
        }

        TileStyle.Pill -> Row(
            modifier = tileModifier.padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrinkIcon(drink, size = 36.dp, confirmed = confirmed)
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                TileText(name, 13.sp, primaryText, FontWeight.Medium)
                TileText(
                    "${formatServingSummary(info.quantity, info.unitKey)} · $mg",
                    11.sp,
                    secondaryText,
                    FontWeight.Normal,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            if (!confirmed) AddBadge(size = 32.dp)
        }
    }
}

@Composable
private fun TileText(text: String, size: TextUnit, color: ColorProvider, weight: FontWeight) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight),
        maxLines = 1,
    )
}

@Composable
private fun DrinkIcon(drink: WidgetDrink, size: Dp, confirmed: Boolean) {
    val palette = widgetPalette
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .background(if (confirmed) palette.onAccent else palette.iconContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            confirmed -> Image(
                provider = ImageProvider(R.drawable.ic_check),
                contentDescription = null,
                modifier = GlanceModifier.size(size * 0.55f),
                colorFilter = ColorFilter.tint(palette.accent),
            )
            drink.icon != null -> Image(
                provider = ImageProvider(drink.icon),
                contentDescription = null,
                modifier = GlanceModifier.size(size * 0.72f),
                contentScale = ContentScale.Fit,
            )
            else -> Text(
                text = drink.info.emoji.ifBlank { "☕" },
                style = TextStyle(fontSize = (size.value * 0.45f).sp),
            )
        }
    }
}

@Composable
private fun AddBadge(size: Dp) {
    val palette = widgetPalette
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .background(palette.accent),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.62f),
            colorFilter = ColorFilter.tint(palette.onAccent),
        )
    }
}

@Composable
internal fun EmptyDrinks(showHint: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val palette = widgetPalette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_no_recent_drinks),
            style = TextStyle(color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        if (showHint) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = context.getString(R.string.widget_no_recent_drinks_hint),
                style = TextStyle(color = palette.textMuted, fontSize = 11.sp),
                maxLines = 2,
            )
        }
    }
}

internal fun Double.toWidgetNumberString(): String =
    if (this < 1.0) "0" else "%.1f".format(this)

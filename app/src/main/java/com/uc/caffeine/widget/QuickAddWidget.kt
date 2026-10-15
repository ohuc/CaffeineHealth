package com.uc.caffeine.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.uc.caffeine.R

/**
 * "Quick Log": one-tap logging of recent drinks. Short sizes lay the drinks out side by side;
 * taller ones switch to a titled list of full-width rows.
 */
class QuickAddWidget : CaffeineGlanceWidget() {

    override val previewSizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 80.dp), DpSize(180.dp, 72.dp), DpSize(250.dp, 72.dp))
    )

    @Composable
    override fun Content(data: WidgetData, confirmedDrinkKey: String?, isPreview: Boolean) {
        val size = LocalSize.current
        WidgetSurface {
            when {
                data.recentDrinks.isEmpty() -> EmptyDrinks(
                    showHint = size.height >= 90.dp,
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                )
                size.height >= 140.dp -> DrinkList(data.recentDrinks, confirmedDrinkKey, size)
                else -> DrinkRow(data.recentDrinks, confirmedDrinkKey, size)
            }
        }
    }
}

@Composable
private fun DrinkRow(drinks: List<WidgetDrink>, confirmedKey: String?, size: DpSize) {
    val innerWidth = size.width - TileGap * 2
    val innerHeight = size.height - TileGap * 2
    val count = tilesThatFit(innerWidth, minTile = 88.dp, max = drinks.size).coerceAtLeast(1)
    val tileWidth = (innerWidth - TileGap * (count - 1)) / count
    val style = tileStyleFor(tileWidth, innerHeight)
    Row(modifier = GlanceModifier.fillMaxSize().padding(TileGap)) {
        drinks.take(count).forEachIndexed { index, drink ->
            if (index > 0) Spacer(GlanceModifier.width(TileGap))
            DrinkTile(
                drink = drink,
                style = style,
                confirmed = drink.key == confirmedKey,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun DrinkList(drinks: List<WidgetDrink>, confirmedKey: String?, size: DpSize) {
    val padding = 10.dp
    val headerHeight = 30.dp
    val rowHeight = 54.dp
    val available = size.height - padding * 2 - headerHeight
    val count = tilesThatFit(available, minTile = rowHeight, max = drinks.size).coerceAtLeast(1)
    val style = tileStyleFor(size.width - padding * 2, rowHeight)
    Column(modifier = GlanceModifier.fillMaxSize().padding(padding)) {
        ListHeader(height = headerHeight)
        drinks.take(count).forEachIndexed { index, drink ->
            if (index > 0) Spacer(GlanceModifier.height(TileGap))
            DrinkTile(
                drink = drink,
                style = style,
                confirmed = drink.key == confirmedKey,
                modifier = GlanceModifier.fillMaxWidth().height(rowHeight),
            )
        }
    }
}

@Composable
private fun ListHeader(height: Dp) {
    val context = LocalContext.current
    val palette = widgetPalette
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(height).padding(start = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetLogo()
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = context.getString(R.string.widget_quick_log_title),
            style = TextStyle(color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

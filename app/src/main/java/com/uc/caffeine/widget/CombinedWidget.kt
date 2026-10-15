package com.uc.caffeine.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width

/**
 * "Caffeine Dashboard": the level panel and quick-log tiles together. One row high, the tiles sit
 * beside the level; taller, they move into a strip under it so the curve gets the full width.
 */
class CombinedWidget : CaffeineGlanceWidget() {

    // One side tile needs 222dp and two need 312dp, so the picker gets a layout for each count.
    override val previewSizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 80.dp), DpSize(222.dp, 80.dp), DpSize(312.dp, 80.dp), DpSize(360.dp, 80.dp))
    )

    @Composable
    override fun Content(data: WidgetData, confirmedDrinkKey: String?, isPreview: Boolean) {
        val size = LocalSize.current
        WidgetSurface {
            if (size.height >= 150.dp) {
                StackedDashboard(data, confirmedDrinkKey, isPreview)
            } else {
                SideBySideDashboard(data, confirmedDrinkKey, isPreview)
            }
        }
    }
}

private val MinLevelPanelWidth = 132.dp
private val SideTileWidth = 84.dp

@Composable
private fun SideBySideDashboard(data: WidgetData, confirmedKey: String?, isPreview: Boolean) {
    val size = LocalSize.current
    val tileArea = size.width - MinLevelPanelWidth - TileGap
    val count = tilesThatFit(tileArea, minTile = SideTileWidth, max = minOf(data.recentDrinks.size, 3))
    // Each tile is followed by a gap, which leaves the trailing one against the widget edge.
    val tilesWidth = (SideTileWidth + TileGap) * count
    val panelWidth = size.width - tilesWidth

    Row(modifier = GlanceModifier.fillMaxSize()) {
        LevelPanel(
            data = data,
            width = panelWidth,
            height = size.height,
            showRefresh = !isPreview && panelWidth >= 160.dp,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        )
        if (count > 0) {
            val style = tileStyleFor(SideTileWidth, size.height - TileGap * 2)
            Row(modifier = GlanceModifier.fillMaxHeight().padding(vertical = TileGap)) {
                data.recentDrinks.take(count).forEach { drink ->
                    DrinkTile(
                        drink = drink,
                        style = style,
                        confirmed = drink.key == confirmedKey,
                        modifier = GlanceModifier.width(SideTileWidth).fillMaxHeight(),
                    )
                    Spacer(GlanceModifier.width(TileGap))
                }
            }
        }
    }
}

@Composable
private fun StackedDashboard(data: WidgetData, confirmedKey: String?, isPreview: Boolean) {
    val size = LocalSize.current
    val stripHeight = if (size.height >= 200.dp) 64.dp else 56.dp
    val innerWidth = size.width - TileGap * 2
    val count = tilesThatFit(innerWidth, minTile = 96.dp, max = minOf(data.recentDrinks.size, 3))
    val tileWidth = if (count == 0) innerWidth else (innerWidth - TileGap * (count - 1)) / count
    val panelHeight = if (count == 0) size.height else size.height - stripHeight - TileGap

    Column(modifier = GlanceModifier.fillMaxSize()) {
        LevelPanel(
            data = data,
            width = size.width,
            height = panelHeight,
            showRefresh = !isPreview,
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
        )
        if (count > 0) {
            val style = tileStyleFor(tileWidth, stripHeight)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(stripHeight + TileGap)
                    .padding(start = TileGap, end = TileGap, bottom = TileGap),
            ) {
                data.recentDrinks.take(count).forEachIndexed { index, drink ->
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
    }
}

/** Header, readout and bedtime outlook over the decay curve. [width]/[height] size the chart. */
@Composable
private fun LevelPanel(
    data: WidgetData,
    width: Dp,
    height: Dp,
    showRefresh: Boolean,
    modifier: GlanceModifier,
) {
    val compact = height < 116.dp
    Box(modifier = modifier) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            ChartLayer(data = data, width = width, height = height * 0.58f)
        }
        Column(modifier = GlanceModifier.fillMaxSize().padding(if (compact) 12.dp else 14.dp)) {
            LevelHeader(showRefresh = showRefresh)
            LevelReadout(data = data, fontSize = if (compact) 24.sp else 32.sp)
            if (!compact) {
                Spacer(GlanceModifier.defaultWeight())
                BedtimeLine(data = data)
            }
        }
    }
}

class CombinedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CombinedWidget()
}

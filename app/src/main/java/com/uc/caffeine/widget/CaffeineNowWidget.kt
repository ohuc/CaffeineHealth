package com.uc.caffeine.widget

import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding

/** "Caffeine Level": the current active level over the decay curve, plus the bedtime outlook. */
class CaffeineNowWidget : CaffeineGlanceWidget() {

    // Past the min-size fallback, each is at least 72dp tall so the preview keeps its header.
    override val previewSizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 50.dp), DpSize(140.dp, 72.dp), DpSize(170.dp, 80.dp), DpSize(200.dp, 90.dp))
    )

    @Composable
    override fun Content(data: WidgetData, confirmedDrinkKey: String?, isPreview: Boolean) {
        val size = LocalSize.current
        // Below ~72dp only the readout fits; below ~116dp the bedtime line is dropped.
        val tiny = size.height < 72.dp
        val compact = size.height < 116.dp
        val readoutSize = when {
            tiny -> 22.sp
            compact -> 26.sp
            size.height >= 160.dp && size.width >= 200.dp -> 42.sp
            else -> 34.sp
        }

        WidgetSurface {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                ChartLayer(data = data, width = size.width, height = size.height * 0.58f)
            }
            if (tiny) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    LevelReadout(data = data, fontSize = readoutSize)
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp)) {
                    LevelHeader(showRefresh = !isPreview && size.width >= 140.dp)
                    if (!compact) Spacer(GlanceModifier.height(4.dp))
                    LevelReadout(data = data, fontSize = readoutSize)
                    if (!compact) {
                        Spacer(GlanceModifier.defaultWeight())
                        BedtimeLine(data = data)
                    }
                }
            }
        }
    }
}

class CaffeineNowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaffeineNowWidget()
}

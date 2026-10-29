package com.uc.caffeine.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.uc.caffeine.R
import kotlin.math.roundToInt

/** "Caffeine Ring": the Home screen's circular view (#57) — active level against the sleep threshold. */
class CaffeineRingWidget : CaffeineGlanceWidget() {

    override val previewSizeMode = SizeMode.Responsive(
        setOf(DpSize(90.dp, 90.dp), DpSize(140.dp, 140.dp), DpSize(200.dp, 200.dp))
    )

    @Composable
    override fun Content(data: WidgetData, confirmedDrinkKey: String?, isPreview: Boolean) {
        val context = LocalContext.current
        val palette = widgetPalette
        val size = LocalSize.current
        val padding = if (min(size.width, size.height) < 110.dp) 8.dp else 12.dp
        val ring = (min(size.width, size.height) - padding * 2).coerceAtLeast(40.dp)
        // Below ~100dp the threshold line no longer fits inside the ring.
        val showThreshold = ring >= 100.dp
        val readoutSize = (ring.value * 0.2f).coerceIn(16f, 44f).sp

        val density = context.resources.displayMetrics.density
        // Capped so a stretched tablet widget doesn't ship a multi-megabyte bitmap per update.
        val ringPx = (ring.value * density).toInt().coerceIn(1, 800)
        val progress = ringProgress(data.currentCaffeineMg, data.settings.sleepThresholdMg)
        val bitmaps = remember(progress, ringPx) { WidgetRingRenderer.render(progress, ringPx) }
        val value = data.currentCaffeineMg.roundToInt().toString()

        WidgetSurface {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = GlanceModifier.size(ring), contentAlignment = Alignment.Center) {
                    bitmaps.track?.let {
                        Image(
                            provider = ImageProvider(it),
                            contentDescription = null,
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(palette.ringTrack),
                        )
                    }
                    bitmaps.progress?.let {
                        Image(
                            provider = ImageProvider(it),
                            contentDescription = null,
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(palette.accent),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ReadoutText(
                            value = value,
                            suffix = null,
                            fontSize = readoutSize,
                            color = palette.text,
                            contentDescription = context.getString(R.string.widget_level_cd, value),
                        )
                        Text(
                            text = context.getString(R.string.home_circular_mg_unit),
                            style = TextStyle(
                                color = palette.textMuted,
                                fontSize = (readoutSize.value * 0.42f).sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                        if (showThreshold) {
                            Text(
                                text = context.getString(
                                    R.string.home_circular_threshold_format,
                                    data.settings.sleepThresholdMg,
                                ),
                                style = TextStyle(color = palette.textMuted, fontSize = 11.sp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

class CaffeineRingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaffeineRingWidget()
}

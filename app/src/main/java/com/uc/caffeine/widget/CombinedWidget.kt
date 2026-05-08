package com.uc.caffeine.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.uc.caffeine.MainActivity
import com.uc.caffeine.R
import com.uc.caffeine.util.formatTimestampToTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CombinedWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { WidgetDataRepository(context).load() }
        val bedtimeLabel = formatTimestampToTime(data.bedtimeMillis, data.settings)
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                CombinedContent(data = data, bedtimeLabel = bedtimeLabel)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetDataRepository.mockData()
        val bedtimeLabel = formatTimestampToTime(data.bedtimeMillis, data.settings)
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                CombinedContent(data = data, bedtimeLabel = bedtimeLabel)
            }
        }
    }
}

class CombinedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CombinedWidget()
}

@Composable
private fun CombinedContent(data: WidgetData, bedtimeLabel: String) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val density = ctx.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceIn(400, 3500)
    val heightPx = (size.height.value * density).toInt().coerceIn(150, 1500)

    val primaryArgb = GlanceTheme.colors.primary.getColor(ctx).toArgb()
    val onSurfaceArgb = GlanceTheme.colors.onSurface.getColor(ctx).toArgb()
    val scheme = remember(data.settings.useDynamicColor) {
        resolveWidgetColorScheme(ctx, data.settings.useDynamicColor)
    }
    val markerArgb = scheme.surfaceContainerHighest.toArgb()
    val markerOutlineArgb = scheme.outlineVariant.toArgb()
    val primaryRgb = primaryArgb and 0x00FFFFFF
    val onSurfaceRgb = onSurfaceArgb and 0x00FFFFFF

    val chartBitmap = remember(data, widthPx, heightPx, primaryArgb, onSurfaceArgb, markerArgb) {
        WidgetChartRenderer.render(
            entries = data.allEntries,
            settings = data.settings,
            widthPx = widthPx,
            heightPx = heightPx,
            lineColor = primaryRgb or 0x90000000.toInt(),
            fillTopColor = primaryRgb or 0x36000000,
            bedtimeMillis = data.bedtimeMillis,
            bedtimeLineColor = primaryRgb or 0xB0000000.toInt(),
            currentTimeColor = onSurfaceRgb or 0x66000000,
            markerColor = markerArgb,
            markerOutlineColor = markerOutlineArgb,
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .clickable(onClick = actionStartActivity(Intent(ctx, MainActivity::class.java))),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (chartBitmap != null) {
            Image(
                provider = ImageProvider(chartBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Box(
            modifier = GlanceModifier.fillMaxSize().padding(end = 210.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.Top,
            ) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = GlanceModifier.size(28.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Caffeine Now",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                Row(verticalAlignment = Alignment.Vertical.Bottom) {
                    Text(
                        text = data.currentCaffeineMg.toWidgetNumberString(),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Text(
                        text = "mg",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                if (data.caffeineAtBedtimeMg >= 1.0) {
                    Text(
                        text = "${"%.0f".format(data.caffeineAtBedtimeMg)} mg at $bedtimeLabel",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        if (data.recentDrinks.isEmpty()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(210.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "No recent drinks",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(210.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                items(data.recentDrinks) { drink ->
                    DrinkRow(drink = drink)
                }
            }
        }
    }
}

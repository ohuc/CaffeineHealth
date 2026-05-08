package com.uc.caffeine.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CaffeineNowWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { WidgetDataRepository(context).load() }
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                CaffeineNowContent(data = data, isPreview = false)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetDataRepository.mockData()
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                CaffeineNowContent(data = data, isPreview = true)
            }
        }
    }
}

class CaffeineNowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaffeineNowWidget()
}

@Composable
internal fun CaffeineNowContent(data: WidgetData, isPreview: Boolean = false) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val density = ctx.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceIn(300, 3000)
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
    ) {
        if (chartBitmap != null) {
            Image(
                provider = ImageProvider(chartBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
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
                    text = ctx.getString(R.string.widget_caffeine_now_inline_label),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.Vertical.Bottom) {
                Text(
                    text = data.currentCaffeineMg.toWidgetNumberString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(3.dp))
                Text(
                    text = ctx.getString(R.string.unit_mg),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        if (!isPreview) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(top = 8.dp, end = 8.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                RefreshIconButton()
            }
        }
    }
}

@Composable
internal fun RefreshIconButton() {
    val ctx = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(36.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(18.dp)
            .clickable(actionRunCallback<RefreshWidgetAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = ctx.getString(R.string.widget_refresh_cd),
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
        )
    }
}

internal fun Double.toWidgetMgString(): String =
    if (this < 1.0) "0 mg" else "${"%.1f".format(this)} mg"

internal fun Double.toWidgetNumberString(): String =
    if (this < 1.0) "0" else "%.1f".format(this)

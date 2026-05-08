package com.uc.caffeine.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.uc.caffeine.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuickAddWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 80.dp), DpSize(250.dp, 130.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { WidgetDataRepository(context).load() }
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                QuickAddContent(recentDrinks = data.recentDrinks)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = WidgetDataRepository.mockData()
        provideContent {
            WidgetTheme(useDynamicColor = data.settings.useDynamicColor) {
                QuickAddContent(recentDrinks = data.recentDrinks)
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

@Composable
internal fun QuickAddContent(recentDrinks: List<WidgetDrink>) {
    val ctx = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .clickable(onClick = actionStartActivity(Intent(ctx, MainActivity::class.java))),
        contentAlignment = Alignment.Center,
    ) {
        if (recentDrinks.isEmpty()) {
            Text(
                text = "No recent drinks",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                ),
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                items(recentDrinks) { drink ->
                    DrinkRow(drink = drink)
                }
            }
        }
    }
}

@Composable
internal fun DrinkRow(drink: WidgetDrink) {
    Box(
        modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 10.dp, bottom = 10.dp, end = 54.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (drink.icon != null) {
                    Image(
                        provider = ImageProvider(drink.icon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(30.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = drink.info.emoji.ifBlank { "☕" },
                        style = TextStyle(fontSize = 22.sp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column(modifier = GlanceModifier.wrapContentHeight()) {
                Text(
                    text = drink.info.drinkName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = "${drink.info.quantity} ${drink.info.unitKey}, ${drink.info.caffeineMg} mg",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(22.dp)
                .clickable(
                    onClick = actionRunCallback<QuickLogAction>(
                        actionParametersOf(
                            QuickLogAction.KEY_PRESET_ID to drink.info.presetItemId,
                            QuickLogAction.KEY_DRINK_NAME to drink.info.drinkName,
                            QuickLogAction.KEY_CAFFEINE_MG to drink.info.caffeineMg,
                            QuickLogAction.KEY_QUANTITY to drink.info.quantity,
                            QuickLogAction.KEY_UNIT_KEY to drink.info.unitKey,
                            QuickLogAction.KEY_UNIT_CAFFEINE_MG to drink.info.unitCaffeineMg,
                            QuickLogAction.KEY_EMOJI to drink.info.emoji,
                            QuickLogAction.KEY_IMAGE_NAME to drink.info.imageName,
                            QuickLogAction.KEY_ABSORPTION_RATE to drink.info.absorptionRate,
                            QuickLogAction.KEY_DURATION_MINUTES to drink.info.durationMinutes,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

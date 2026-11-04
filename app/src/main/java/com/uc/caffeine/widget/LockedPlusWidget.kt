package com.uc.caffeine.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.uc.caffeine.MainActivity
import com.uc.caffeine.R

@Composable
internal fun LockedPlusWidget() {
    val context = LocalContext.current
    WidgetSurface {
        Box(GlanceModifier.fillMaxSize().padding(12.dp).clickable(actionStartActivity(
            Intent(context, MainActivity::class.java).putExtra("plus_feature", "PremiumWidgets"))),
            contentAlignment = Alignment.Center) {
            Text(context.getString(R.string.plus_see_plans), style = TextStyle(color = widgetPalette.text))
        }
    }
}

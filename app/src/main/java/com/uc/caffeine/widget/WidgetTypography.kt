package com.uc.caffeine.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.uc.caffeine.R
import kotlin.math.ceil

/**
 * Montserrat for widget display text.
 *
 * RemoteViews can't take a Typeface: there is no setter, Glance's `FontFamily` only resolves
 * system families, and `android:fontFamily="@font/…"` is ignored because the launcher inflates
 * widget layouts in a restricted context. The only route to the app's font is drawing the text
 * ourselves, so this renders it as a white alpha mask and lets the launcher tint it. The tint is a
 * day/night pair, which keeps the text in step with the system theme like ordinary widget text.
 *D
 * Reserved for short display strings (the level readout); body text stays as real `Text` so it
 * ellipsizes, wraps and reaches TalkBack normally.
 */
internal object WidgetTypography {
    @Volatile private var montserrat: Typeface? = null

    private fun typeface(context: Context): Typeface =
        montserrat ?: context.resources.getFont(R.font.montserrat_variable).also { montserrat = it }

    /**
     * Draws [primary] followed by a smaller [suffix] that shares its baseline ("142.6 mg").
     * Sizes go through the user's font scale, so the readout grows with their other text.
     */
    fun renderReadout(
        context: Context,
        primary: String,
        suffix: String?,
        fontSize: TextUnit,
        suffixScale: Float = 0.46f,
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val primaryPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize.value, metrics)
        val primaryPaint = maskPaint(context, primaryPx, weight = 700)
        val suffixPaint = maskPaint(context, primaryPx * suffixScale, weight = 600)

        val gap = if (suffix.isNullOrEmpty()) 0f else primaryPx * 0.1f
        val primaryWidth = primaryPaint.measureText(primary)
        val suffixWidth = suffix?.let(suffixPaint::measureText) ?: 0f
        // Tight vertical bounds: cap height above the baseline, a little descent below so the
        // bitmap sits snug against neighbouring rows instead of carrying the font's line gap.
        val fm = primaryPaint.fontMetrics
        val ascent = -fm.ascent * 0.86f
        val descent = fm.descent * 0.55f

        val width = ceil(primaryWidth + gap + suffixWidth).toInt().coerceAtLeast(1)
        val height = ceil(ascent + descent).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawText(primary, 0f, ascent, primaryPaint)
            if (!suffix.isNullOrEmpty()) drawText(suffix, primaryWidth + gap, ascent, suffixPaint)
        }
        return bitmap
    }

    private fun maskPaint(context: Context, sizePx: Float, weight: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            typeface = typeface(context)
            textSize = sizePx
            fontVariationSettings = "'wght' $weight"
        }
}

/** A display readout in Montserrat; see [WidgetTypography] for why it is an image. */
@Composable
internal fun ReadoutText(
    value: String,
    suffix: String?,
    fontSize: TextUnit,
    color: ColorProvider,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val bitmap = remember(value, suffix, fontSize) {
        WidgetTypography.renderReadout(context, value, suffix, fontSize)
    }
    val density = context.resources.displayMetrics.density
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = contentDescription,
        modifier = modifier
            .width((bitmap.width / density).dp)
            .height((bitmap.height / density).dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color),
    )
}

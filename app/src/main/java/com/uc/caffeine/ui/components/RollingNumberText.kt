package com.uc.caffeine.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.unit.Constraints

@Composable
fun RollingNumberText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Bottom,
    labelPrefix: String = "rolling_number",
) {
    val resolvedColor = if (style.color == Color.Unspecified) {
        LocalContentColor.current
    } else {
        style.color
    }
    val resolvedStyle = style.copy(
        color = resolvedColor,
        fontWeight = style.fontWeight ?: FontWeight.Bold,
    )

    Layout(
        // Read and found as one value rather than character by character.
        modifier = modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        measurePolicy = NumberFitPolicy,
        content = {
        Row(horizontalArrangement = horizontalArrangement, verticalAlignment = verticalAlignment) {
        text.forEachIndexed { index, char ->
            // Keyed from the end so the ones digit keeps its slot (and rolls) when the number
            // gains or loses digits, instead of every digit shifting one place over.
            key(text.length - index) {
                if (char.isDigit()) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "${labelPrefix}_digit_$index",
                    ) { digit ->
                        Text(
                            text = digit.toString(),
                            style = resolvedStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                } else {
                    Text(
                        text = char.toString(),
                        style = resolvedStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
        }
        Text(text = text, style = resolvedStyle)
    })
}

private object NumberFitPolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val child = if (measurables[0].maxIntrinsicWidth(Constraints.Infinity) <= constraints.maxWidth) measurables[0] else measurables[1]
        val placed = child.measure(constraints)
        return layout(placed.width, placed.height) { placed.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables[1].minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables[0].maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        fittingChild(measurables, width).minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        fittingChild(measurables, width).maxIntrinsicHeight(width)

    private fun fittingChild(measurables: List<IntrinsicMeasurable>, width: Int) =
        if (measurables[0].maxIntrinsicWidth(Constraints.Infinity) <= width) measurables[0] else measurables[1]
}

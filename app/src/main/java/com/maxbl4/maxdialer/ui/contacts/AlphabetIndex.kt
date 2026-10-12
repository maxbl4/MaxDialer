package com.maxbl4.maxdialer.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val SLOT_HEIGHT = 16.dp
private val BUBBLE_SIZE = 64.dp

/**
 * One UI fast-scroll index: tap or drag along the letters to jump to a section.
 * When not all letters fit, every n-th is shown with dots between, but dragging still reaches each one.
 */
@Composable
fun AlphabetIndex(
    letters: List<Char>,
    current: Char?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val select by rememberUpdatedState(onSelect)
    val count by rememberUpdatedState(letters.size)
    var active by remember { mutableStateOf<Int?>(null) }
    var touchY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier.fillMaxHeight()) {
        val heightPx = constraints.maxHeight.toFloat()
        val slots = (maxHeight / SLOT_HEIGHT).toInt().coerceAtLeast(3)
        val labels = remember(letters, slots) { visibleLabels(letters, slots) }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight()
                .pointerInput(heightPx) {
                    fun update(y: Float) {
                        touchY = y.coerceIn(0f, heightPx)
                        val i = (touchY / heightPx * count).toInt().coerceIn(0, count - 1)
                        if (i != active) {
                            active = i
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            select(i)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        update(down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            update(change.position.y)
                        }
                        active = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (label in labels) {
                Text(
                    label.toString(),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (label == current) FontWeight.Bold else FontWeight.Normal,
                    color = if (label == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val shown = active
        if (shown != null) {
            val bubble = with(LocalDensity.current) { BUBBLE_SIZE.toPx() }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-48.dp.roundToPx(), (touchY - bubble / 2).coerceIn(0f, heightPx - bubble).roundToInt()) }
                    .size(BUBBLE_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letters[shown].toString(),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val DOT = '•'

/** All letters if they fit in [slots], otherwise evenly picked letters with dots between them. */
internal fun visibleLabels(letters: List<Char>, slots: Int): List<Char> {
    if (letters.size <= slots) return letters
    val shown = (slots + 1) / 2
    return buildList {
        for (k in 0 until shown) {
            if (k > 0) add(DOT)
            add(letters[(k.toLong() * (letters.size - 1) / (shown - 1).coerceAtLeast(1)).toInt()])
        }
    }
}

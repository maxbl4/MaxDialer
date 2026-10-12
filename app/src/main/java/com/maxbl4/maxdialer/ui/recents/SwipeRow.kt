package com.maxbl4.maxdialer.ui.recents

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.maxbl4.maxdialer.ui.theme.OneUi
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TRIGGER_FRACTION = 0.3f

/** Row that calls when swiped right and opens messages when swiped left, then slides back. */
@Composable
fun SwipeCallSmsRow(
    enabled: Boolean,
    shape: Shape,
    onCall: () -> Unit,
    onSms: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var width by remember { mutableIntStateOf(1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val threshold = width * TRIGGER_FRACTION

    val dragState = rememberDraggableState { delta ->
        val before = abs(offset) >= threshold
        offset = (offset + delta).coerceIn(-width.toFloat(), width.toFloat())
        if (!before && abs(offset) >= threshold) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    Box(modifier.clip(shape).onSizeChanged { width = it.width }) {
        if (offset != 0f) {
            val right = offset > 0
            Box(
                Modifier
                    .matchParentSize()
                    .background(if (right) OneUi.callGreen else OneUi.smsBlue)
                    .padding(horizontal = 28.dp),
                contentAlignment = if (right) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(if (right) Icons.Filled.Call else Icons.AutoMirrored.Filled.Message, null, tint = Color.White)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    dragState,
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        scope.launch {
                            when {
                                offset >= threshold -> onCall()
                                offset <= -threshold -> onSms()
                            }
                            animate(offset, 0f) { value, _ -> offset = value }
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

package com.maxbl4.maxdialer.ui.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.SimAccount
import com.maxbl4.maxdialer.search.T9
import com.maxbl4.maxdialer.ui.theme.OneUi

/** Typed number with paste on long press and «Add to contacts» under it. */
@Composable
fun NumberDisplay(
    text: String,
    showAddContact: Boolean,
    onPaste: () -> Unit,
    onAddContact: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { menu = true })
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Text(
                text,
                fontSize = if (text.length > 15) 24.sp else 30.sp,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(40.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.paste)) }, onClick = { menu = false; onPaste() })
            }
        }
        Text(
            stringResource(R.string.add_to_contacts),
            color = if (showAddContact) MaterialTheme.colorScheme.primary else Color.Transparent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(enabled = showAddContact, onClick = onAddContact)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private val keys = listOf("123", "456", "789", "*0#")

/** Long press alternatives: 0 → +, * → pause, # → wait. */
private val longPress = mapOf('0' to '+', '*' to ',', '#' to ';')

@Composable
fun DialpadKeys(onKey: (Char) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
        for (row in keys) {
            Row(Modifier.fillMaxWidth()) {
                for (key in row) {
                    DialpadKey(
                        key,
                        Modifier.weight(1f),
                        onClick = { onKey(key) },
                        onLongClick = longPress[key]?.let { alt ->
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onKey(alt)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialpadKey(key: Char, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(32.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(key.toString(), fontSize = 30.sp, lineHeight = 32.sp)
        val digit = key - '0'
        when {
            digit in 2..9 -> {
                Text(T9.latinLetters[digit], fontSize = 10.sp, lineHeight = 11.sp, color = secondary, letterSpacing = 1.sp)
                Text(T9.cyrillicLetters[digit], fontSize = 10.sp, lineHeight = 11.sp, color = secondary, letterSpacing = 1.sp)
            }
            key == '0' -> Text("+", fontSize = 13.sp, lineHeight = 14.sp, color = secondary)
            else -> Spacer(Modifier.height(14.dp))
        }
    }
}

/** Collapse, call (one button per SIM on dual-SIM phones), backspace. */
@Composable
fun DialpadActions(
    sims: List<SimAccount>,
    showBackspace: Boolean,
    onCollapse: () -> Unit,
    onCall: (SimAccount?) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            RoundIcon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.hide_dialpad), onClick = onCollapse)
        }
        if (sims.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (sim in sims) SimCallButton(sim) { onCall(sim) }
            }
        } else {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OneUi.callGreen)
                    .combinedClickable(onClick = { onCall(null) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Call, stringResource(R.string.action_call), tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (showBackspace) {
                RoundIcon(
                    Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.backspace),
                    onClick = onBackspace, onLongClick = onClear,
                )
            }
        }
    }
}

@Composable
private fun SimCallButton(sim: SimAccount, onClick: () -> Unit) {
    Row(
        Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(OneUi.callGreen)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(sim.slot.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

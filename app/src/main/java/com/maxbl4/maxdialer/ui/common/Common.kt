package com.maxbl4.maxdialer.ui.common

import android.graphics.BitmapFactory
import android.provider.CallLog
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.ui.theme.OneUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val photoCache = LruCache<String, ImageBitmap>(200)

@Composable
fun Avatar(name: String?, photoUri: String?, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    var photo by remember(photoUri) { mutableStateOf(photoUri?.let { photoCache.get(it) }) }
    LaunchedEffect(photoUri) {
        if (photoUri == null || photo != null) return@LaunchedEffect
        photo = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(photoUri.toUri())?.use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }?.also { photoCache.put(photoUri, it) }
    }

    val letter = name?.firstOrNull { it.isLetter() }?.uppercaseChar()
    val color = if (name == null) Color(0xFFB4B4B4) else OneUi.avatarColors[Math.floorMod(name.hashCode(), OneUi.avatarColors.size)]
    Box(modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        val bitmap = photo
        when {
            bitmap != null -> Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            letter != null -> Text(letter.toString(), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Medium)
            else -> Icon(Icons.Filled.Person, null, Modifier.size(size * 0.62f), tint = Color.White)
        }
    }
}

fun highlighted(text: String, ranges: List<IntRange>, color: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    for (r in ranges) {
        if (r.first in text.indices && r.last in text.indices) {
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), r.first, r.last + 1)
        }
    }
}

/** Shape of a row inside a One UI rounded card made of several rows. */
fun cardRowShape(index: Int, count: Int): Shape {
    val r = OneUi.cardRadius
    return when {
        count == 1 -> RoundedCornerShape(r)
        index == 0 -> RoundedCornerShape(topStart = r, topEnd = r)
        index == count - 1 -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        else -> RoundedCornerShape(0.dp)
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SimBadge(slot: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 12.dp, height = 15.dp)
            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(slot.toString(), color = MaterialTheme.colorScheme.surface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

fun callTypeIcon(type: Int): ImageVector = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
    CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
    CallLog.Calls.REJECTED_TYPE -> Icons.Filled.CallEnd
    CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block
    CallLog.Calls.VOICEMAIL_TYPE -> Icons.Filled.Voicemail
    else -> Icons.AutoMirrored.Filled.CallReceived
}

fun callTypeColor(type: Int): Color = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> OneUi.outgoingGreen
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> OneUi.missedRed
    else -> OneUi.incomingBlue
}

@Composable
fun callTypeLabel(type: Int): String = stringResource(
    when (type) {
        CallLog.Calls.OUTGOING_TYPE -> R.string.call_outgoing
        CallLog.Calls.MISSED_TYPE -> R.string.call_missed
        CallLog.Calls.REJECTED_TYPE -> R.string.call_rejected
        CallLog.Calls.BLOCKED_TYPE -> R.string.call_blocked
        CallLog.Calls.VOICEMAIL_TYPE -> R.string.call_voicemail
        else -> R.string.call_incoming
    },
)

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
private val dayYearFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

@Composable
fun formatDay(day: LocalDate): String {
    val today = LocalDate.now()
    return when {
        day == today -> stringResource(R.string.today)
        day == today.minusDays(1) -> stringResource(R.string.yesterday)
        day.year == today.year -> day.format(dayFormat)
        else -> day.format(dayYearFormat)
    }
}

@Composable
fun formatDuration(seconds: Long): String =
    if (seconds < 60) stringResource(R.string.duration_sec, seconds)
    else stringResource(R.string.duration_min_sec, seconds / 60, seconds % 60)

package com.maxbl4.maxdialer.ui.details

import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.CallEntry
import com.maxbl4.maxdialer.data.PhoneData
import com.maxbl4.maxdialer.data.PhoneRepository
import com.maxbl4.maxdialer.ui.MenuTarget
import com.maxbl4.maxdialer.ui.common.Avatar
import com.maxbl4.maxdialer.ui.common.SectionHeader
import com.maxbl4.maxdialer.ui.common.SimBadge
import com.maxbl4.maxdialer.ui.common.callTypeColor
import com.maxbl4.maxdialer.ui.common.callTypeIcon
import com.maxbl4.maxdialer.ui.common.callTypeLabel
import com.maxbl4.maxdialer.ui.common.cardRowShape
import com.maxbl4.maxdialer.ui.common.formatDay
import com.maxbl4.maxdialer.ui.common.formatDuration
import com.maxbl4.maxdialer.ui.common.formatTime
import com.maxbl4.maxdialer.ui.common.localDate
import com.maxbl4.maxdialer.ui.recents.RoundIcon
import com.maxbl4.maxdialer.ui.theme.OneUi

/** Card of one number: who it is, quick actions and every call with it. */
@Composable
fun CallDetailsScreen(
    key: String,
    data: PhoneData,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onContact: (MenuTarget) -> Unit,
    onMenu: (MenuTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calls = data.calls.filter { it.key == key }
    val first = calls.firstOrNull()
    val contact = if (first?.isPrivate == false) data.contactsByKey[key] else null
    val number = contact?.number ?: first?.number ?: ""
    val displayNumber = PhoneRepository.formatNumber(number, data.countryIso)
    val target = MenuTarget(number, displayNumber, contact, calls)
    val context = LocalContext.current

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onClick = onBack)
            Spacer(Modifier.weight(1f))
            if (first != null) RoundIcon(Icons.Filled.MoreVert, null, onClick = { onMenu(target) })
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(contact?.name, contact?.photoUri, size = 96.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when {
                            first?.isPrivate == true -> stringResource(R.string.private_number)
                            contact != null -> contact.name
                            else -> displayNumber
                        },
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    if (contact != null) {
                        Text(
                            "${Phone.getTypeLabel(context.resources, contact.type, contact.label)}  $displayNumber",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            if (first != null && !first.isPrivate) {
                item {
                    Row(
                        Modifier
                            .padding(horizontal = OneUi.screenPadding)
                            .clip(RoundedCornerShape(OneUi.cardRadius))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ActionButton(Icons.Filled.Call, stringResource(R.string.action_call), OneUi.callGreen) { onCall(number) }
                        ActionButton(Icons.AutoMirrored.Filled.Message, stringResource(R.string.action_message), OneUi.smsBlue) { onSms(number) }
                        if (contact != null) {
                            ActionButton(Icons.Filled.Person, stringResource(R.string.action_contact), MaterialTheme.colorScheme.onSurfaceVariant) { onContact(target) }
                        } else {
                            ActionButton(Icons.Filled.PersonAdd, stringResource(R.string.action_add_contact), MaterialTheme.colorScheme.onSurfaceVariant) { onContact(target) }
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.details_history)) }
            itemsIndexed(calls, key = { _, c -> c.id }) { i, call -> HistoryRow(call, data, i, calls.size) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(26.dp), tint = color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun HistoryRow(call: CallEntry, data: PhoneData, index: Int, count: Int) {
    Column(
        Modifier
            .padding(horizontal = OneUi.screenPadding)
            .clip(cardRowShape(index, count))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Icon(callTypeIcon(call.type), null, Modifier.size(20.dp), tint = callTypeColor(call.type))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(callTypeLabel(call.type), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                Text(
                    "${formatDay(localDate(call.date))}, ${formatTime(call.date)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            if (call.duration > 0) {
                Text(formatDuration(call.duration), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            data.simSlot(call.accountId)?.let {
                Spacer(Modifier.width(8.dp))
                SimBadge(it)
            }
        }
        if (index < count - 1) {
            HorizontalDivider(Modifier.padding(start = 64.dp, end = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

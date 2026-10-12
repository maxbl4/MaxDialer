package com.maxbl4.maxdialer.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.ContactPhone
import com.maxbl4.maxdialer.ui.common.Avatar
import com.maxbl4.maxdialer.ui.common.SectionHeader
import com.maxbl4.maxdialer.ui.common.cardRowShape
import com.maxbl4.maxdialer.ui.recents.RoundIcon
import com.maxbl4.maxdialer.ui.theme.OneUi

/** Contacts with phone numbers, Russian names first, grouped by first letter. */
@Composable
fun ContactsScreen(
    contacts: List<ContactPhone>,
    loaded: Boolean,
    onOpen: (ContactPhone) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(contacts) {
        contacts.distinctBy { it.contactId }
            .groupBy { c -> c.name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
            .toList()
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_contacts), Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            RoundIcon(Icons.Filled.Add, stringResource(R.string.menu_create_contact), onClick = onCreate)
        }
        if (loaded && sections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_contacts), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            for ((letter, group) in sections) {
                item(key = "letter-$letter") { SectionHeader(letter.toString()) }
                itemsIndexed(group, key = { _, c -> c.contactId }) { i, contact ->
                    Column(
                        Modifier
                            .padding(horizontal = OneUi.screenPadding)
                            .clip(cardRowShape(i, group.size))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpen(contact) },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(contact.name, contact.photoUri, size = 40.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(contact.name, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (i < group.size - 1) {
                            HorizontalDivider(Modifier.padding(start = 70.dp, end = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

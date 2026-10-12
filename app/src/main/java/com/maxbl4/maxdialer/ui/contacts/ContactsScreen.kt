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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.ContactPhone
import com.maxbl4.maxdialer.search.TextSearch
import com.maxbl4.maxdialer.search.numberVariants
import com.maxbl4.maxdialer.ui.common.Avatar
import com.maxbl4.maxdialer.ui.common.SectionHeader
import com.maxbl4.maxdialer.ui.common.cardRowShape
import com.maxbl4.maxdialer.ui.recents.RoundIcon
import com.maxbl4.maxdialer.ui.theme.OneUi

/** Contacts with phone numbers, Russian names first, grouped by first letter, with search. */
@Composable
fun ContactsScreen(
    contacts: List<ContactPhone>,
    loaded: Boolean,
    onOpen: (ContactPhone) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val unique = remember(contacts) { contacts.distinctBy { it.contactId } }
    val sections = remember(unique) {
        unique.groupBy { c -> c.name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
            .toList()
    }
    val results = remember(contacts, query) { if (query.isBlank()) emptyList() else searchContacts(contacts, query) }

    val focus = LocalFocusManager.current
    val hideKeyboardOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) focus.clearFocus()
                return Offset.Zero
            }
        }
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_contacts), Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            RoundIcon(Icons.Filled.Add, stringResource(R.string.menu_create_contact), onClick = onCreate)
        }
        SearchField(query, { query = it }, Modifier.padding(horizontal = OneUi.screenPadding, vertical = 8.dp))
        val listModifier = Modifier.fillMaxSize().nestedScroll(hideKeyboardOnScroll)
        when {
            query.isNotBlank() && results.isEmpty() -> EmptyText(stringResource(R.string.no_matches))
            query.isNotBlank() -> LazyColumn(listModifier, contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
                contactRows(results, onOpen)
            }
            loaded && sections.isEmpty() -> EmptyText(stringResource(R.string.no_contacts))
            else -> Box(Modifier.fillMaxSize()) {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                // List index of each section header: a header plus its rows per section.
                val headerIndexes = remember(sections) {
                    sections.runningFold(0) { index, (_, group) -> index + 1 + group.size }.dropLast(1)
                }
                val current by remember(sections, headerIndexes) {
                    derivedStateOf {
                        val first = listState.firstVisibleItemIndex
                        sections.getOrNull(headerIndexes.indexOfLast { it <= first })?.first
                    }
                }
                LazyColumn(listModifier, listState, contentPadding = PaddingValues(end = INDEX_WIDTH, bottom = 24.dp)) {
                    for ((letter, group) in sections) {
                        item(key = "letter-$letter") { SectionHeader(letter.toString()) }
                        contactRows(group, onOpen)
                    }
                }
                AlphabetIndex(
                    letters = sections.map { it.first },
                    current = current,
                    onSelect = { scope.launch { listState.scrollToItem(headerIndexes[it]) } },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
                )
            }
        }
    }
}

private val INDEX_WIDTH = 16.dp

/**
 * Contacts whose name matches [query] (with layout fix and transliteration), then those whose number
 * contains its digits, one entry per contact.
 */
internal fun searchContacts(contacts: List<ContactPhone>, query: String): List<ContactPhone> {
    val unique = contacts.distinctBy { it.contactId }
    val byName = TextSearch.filter(unique, query) { it.name }
    val digits = query.filter { it.isDigit() }
    if (digits.length < 3 || query.any { !it.isDigit() && it !in "+-() " }) return byName

    val found = byName.mapTo(HashSet()) { it.contactId }
    val byNumber = contacts.filter { c ->
        c.contactId !in found && numberVariants(c.number.filter { it.isDigit() }).any { digits in it.digits }
    }.distinctBy { it.contactId }
    return byName + byNumber
}

private fun LazyListScope.contactRows(group: List<ContactPhone>, onOpen: (ContactPhone) -> Unit) {
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

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = color, fontSize = 17.sp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(22.dp), tint = secondary)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search), color = secondary, fontSize = 17.sp)
                    innerTextField()
                }
                if (query.isNotEmpty()) RoundIcon(Icons.Filled.Close, null, onClick = { onQueryChange("") })
            }
        },
    )
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}

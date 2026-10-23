package com.maxbl4.maxdialer.ui.recents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.maxbl4.maxdialer.ui.common.SearchField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.PhoneData
import com.maxbl4.maxdialer.data.PhoneRepository
import com.maxbl4.maxdialer.data.RecentGroup
import com.maxbl4.maxdialer.data.SearchTarget
import com.maxbl4.maxdialer.data.SimAccount
import com.maxbl4.maxdialer.search.SearchHit
import com.maxbl4.maxdialer.search.numberKey
import com.maxbl4.maxdialer.ui.MenuTarget
import com.maxbl4.maxdialer.ui.common.Avatar
import com.maxbl4.maxdialer.ui.common.SectionHeader
import com.maxbl4.maxdialer.ui.common.SimBadge
import com.maxbl4.maxdialer.ui.common.callTypeColor
import com.maxbl4.maxdialer.ui.common.callTypeIcon
import com.maxbl4.maxdialer.ui.common.callTypeLabel
import com.maxbl4.maxdialer.ui.common.cardRowShape
import com.maxbl4.maxdialer.ui.common.formatDay
import com.maxbl4.maxdialer.ui.common.formatTime
import com.maxbl4.maxdialer.ui.common.formatWhen
import com.maxbl4.maxdialer.ui.common.highlighted
import com.maxbl4.maxdialer.ui.common.localDate
import com.maxbl4.maxdialer.ui.theme.OneUi
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/** Callbacks the main screen needs from the app. */
class RecentsActions(
    val onType: (Char) -> Unit,
    val onBackspace: () -> Unit,
    val onClear: () -> Unit,
    val onPaste: () -> Unit,
    val onCall: (number: String, sim: SimAccount?) -> Unit,
    val onSms: (String) -> Unit,
    val onAddContact: (String) -> Unit,
    val onOpenDetails: (key: String) -> Unit,
    val onMenu: (MenuTarget) -> Unit,
)

/** Call log with the dialpad under it; typing turns the log into T9 search results. */
@Composable
fun RecentsScreen(
    data: PhoneData,
    query: String,
    dial: TextFieldValue,
    onDialChange: (TextFieldValue) -> Unit,
    results: List<SearchHit<SearchTarget>>,
    dialpadOpen: Boolean,
    onDialpadOpenChange: (Boolean) -> Unit,
    actions: RecentsActions,
    modifier: Modifier = Modifier,
) {
    /** Text typed in the call log search, or null when the search is closed. */
    var logQuery by rememberSaveable { mutableStateOf<String?>(null) }
    val searching = logQuery != null
    BackHandler(enabled = searching) { logQuery = null }

    val focus = LocalFocusManager.current
    val currentOpen = rememberUpdatedState(dialpadOpen)
    val collapseOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && abs(available.y) > 2f) {
                    if (currentOpen.value) onDialpadOpenChange(false)
                    focus.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    Column(modifier) {
        val searchText = logQuery
        if (searchText != null) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = OneUi.screenPadding, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onClick = { logQuery = null })
                SearchField(searchText, { logQuery = it }, Modifier.weight(1f).focusRequester(focusRequester))
            }
        } else Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.tab_recents), Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            RoundIcon(Icons.Filled.Search, stringResource(R.string.search), onClick = {
                logQuery = ""
                onDialpadOpenChange(false)
            })
            Box {
                var menu by remember { mutableStateOf(false) }
                RoundIcon(Icons.Filled.MoreVert, null, onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.paste)) },
                        onClick = {
                            menu = false
                            actions.onPaste()
                            onDialpadOpenChange(true)
                        },
                    )
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val listModifier = Modifier.fillMaxSize().nestedScroll(collapseOnScroll)
            when {
                searchText != null -> {
                    val found = remember(data.recents, searchText) { filterRecents(data.recents, searchText) }
                    RecentsList(data, found, stringResource(R.string.no_matches), actions, listModifier)
                }
                query.isEmpty() -> RecentsList(data, data.recents, stringResource(R.string.no_recents), actions, listModifier)
                else -> ResultsList(results, actions, listModifier)
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !dialpadOpen && !searching,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) {
                FloatingActionButton(
                    onClick = { onDialpadOpenChange(true) },
                    shape = CircleShape,
                    containerColor = OneUi.callGreen,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Filled.Dialpad, stringResource(R.string.show_dialpad))
                }
            }
        }
        if (query.isNotEmpty() && !searching) {
            NumberDisplay(
                value = dial,
                onValueChange = onDialChange,
                countryIso = data.countryIso,
                showAddContact = query.count { it.isDigit() } >= 3 && data.contactsByKey[numberKey(query)] == null,
                onAddContact = { actions.onAddContact(query) },
            )
        }
        AnimatedVisibility(visible = dialpadOpen && !searching, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                DialpadKeys(actions.onType)
                DialpadActions(
                    sims = data.sims,
                    showBackspace = query.isNotEmpty(),
                    onCollapse = { onDialpadOpenChange(false) },
                    onCall = { sim -> if (query.isNotEmpty()) actions.onCall(query, sim) },
                    onBackspace = actions.onBackspace,
                    onClear = actions.onClear,
                )
            }
        }
    }
}

private class DaySection(val title: java.time.LocalDate, val groups: List<RecentGroup>)

@Composable
private fun RecentsList(
    data: PhoneData,
    recents: List<RecentGroup>,
    emptyText: String,
    actions: RecentsActions,
    modifier: Modifier,
) {
    val sections = remember(recents) {
        recents.groupBy { localDate(it.latest.date) }.map { (day, groups) -> DaySection(day, groups) }
    }
    if (data.loaded && sections.isEmpty()) {
        EmptyText(emptyText, modifier)
        return
    }
    LazyColumn(modifier, rememberLazyListState(), contentPadding = PaddingValues(bottom = 88.dp)) {
        for (section in sections) {
            item(key = "day-${section.title}") { SectionHeader(formatDay(section.title)) }
            itemsIndexed(section.groups, key = { _, g -> g.latest.id }) { i, group ->
                RecentRow(group, data, i, section.groups.size, actions)
            }
        }
    }
}

@Composable
private fun RecentRow(group: RecentGroup, data: PhoneData, index: Int, count: Int, actions: RecentsActions) {
    val call = group.latest
    val contact = group.contact
    val context = LocalContext.current
    val number = remember(call.number, data.countryIso) { PhoneRepository.formatNumber(call.number, data.countryIso) }
    val title = when {
        call.isPrivate -> stringResource(R.string.private_number)
        contact != null -> contact.name
        else -> number
    }
    val subtitle = if (contact != null) {
        Phone.getTypeLabel(context.resources, contact.type, contact.label).toString()
    } else {
        callTypeLabel(call.type)
    }
    val missed = call.isMissed
    val target = MenuTarget(call.number, number, contact, group.calls)

    SwipeCallSmsRow(
        enabled = !call.isPrivate,
        shape = cardRowShape(index, count),
        onCall = { actions.onCall(call.number, null) },
        onSms = { actions.onSms(call.number) },
        modifier = Modifier.padding(horizontal = OneUi.screenPadding),
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { actions.onOpenDetails(group.key) },
                        onLongClick = { actions.onMenu(target) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(contact?.name, contact?.photoUri)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            Modifier.weight(1f, fill = false),
                            color = if (missed) OneUi.missedRed else MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = if (missed && call.isNew) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (group.calls.size > 1) {
                            Text(
                                " (${group.calls.size})",
                                color = if (missed) OneUi.missedRed else MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(callTypeIcon(call.type), null, Modifier.size(15.dp), tint = callTypeColor(call.type))
                        data.simSlot(call.accountId)?.let {
                            Spacer(Modifier.width(4.dp))
                            SimBadge(it)
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(formatTime(call.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            if (index < count - 1) RowDivider()
        }
    }
}

@Composable
private fun ResultsList(results: List<SearchHit<SearchTarget>>, actions: RecentsActions, modifier: Modifier) {
    if (results.isEmpty()) {
        EmptyText(stringResource(R.string.no_matches), modifier)
        return
    }
    LazyColumn(modifier, contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)) {
        itemsIndexed(results, key = { _, hit -> "${hit.item.value.contact?.contactId}/${hit.item.number}" }) { i, hit ->
            ResultRow(hit, i, results.size, actions)
        }
    }
}

@Composable
private fun ResultRow(hit: SearchHit<SearchTarget>, index: Int, count: Int, actions: RecentsActions) {
    val target = hit.item.value
    val accent = MaterialTheme.colorScheme.primary
    val name = hit.item.name
    SwipeCallSmsRow(
        enabled = true,
        shape = cardRowShape(index, count),
        onCall = { actions.onCall(target.number, null) },
        onSms = { actions.onSms(target.number) },
        modifier = Modifier.padding(horizontal = OneUi.screenPadding),
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { actions.onCall(target.number, null) },
                        onLongClick = { actions.onMenu(MenuTarget(target.number, hit.item.number, target.contact)) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(name, target.contact?.photoUri)
                Spacer(Modifier.width(14.dp))
                val lastCall = target.lastCall
                Column(Modifier.weight(1f)) {
                    val numberText = highlighted(hit.item.number, listOfNotNull(hit.numberHighlight), accent)
                    Text(
                        if (name != null) highlighted(name, hit.nameHighlights, accent) else numberText,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The last call's icon shows the result comes from the call history.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lastCall != null) {
                            Icon(callTypeIcon(lastCall.type), null, Modifier.size(15.dp), tint = callTypeColor(lastCall.type))
                            Spacer(Modifier.width(5.dp))
                        }
                        when {
                            name != null -> Text(numberText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
                            lastCall != null -> Text(callTypeLabel(lastCall.type), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
                if (lastCall != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(formatWhen(lastCall.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            if (index < count - 1) RowDivider()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 74.dp, end = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EmptyText(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}

package com.maxbl4.maxdialer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.CallEntry
import com.maxbl4.maxdialer.data.ContactPhone
import com.maxbl4.maxdialer.data.SimAccount
import com.maxbl4.maxdialer.ui.theme.OneUi

/** What the long-press menu was opened on. */
data class MenuTarget(
    val number: String,
    val displayNumber: String,
    val contact: ContactPhone?,
    /** Call log entries the row stands for; empty for a search result. */
    val calls: List<CallEntry> = emptyList(),
) {
    val isPrivate get() = number.isBlank()
}

sealed interface NumberAction {
    data class Call(val sim: SimAccount?) : NumberAction
    data object Sms : NumberAction
    data object EditBeforeCall : NumberAction
    data object Copy : NumberAction
    data object CreateContact : NumberAction
    data object AddToContact : NumberAction
    data object OpenContact : NumberAction
    data object Block : NumberAction
    data object DeleteEntry : NumberAction
    data object DeleteAll : NumberAction
}

@Composable
fun NumberMenuDialog(
    target: MenuTarget,
    sims: List<SimAccount>,
    onAction: (NumberAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = buildList {
        if (!target.isPrivate) {
            if (sims.size > 1) {
                for (sim in sims) add(stringResource(R.string.menu_call_sim, sim.slot) to NumberAction.Call(sim))
            }
            add(stringResource(R.string.menu_sms) to NumberAction.Sms)
            add(stringResource(R.string.menu_edit_before_call) to NumberAction.EditBeforeCall)
            add(stringResource(R.string.menu_copy) to NumberAction.Copy)
            if (target.contact == null) {
                add(stringResource(R.string.menu_create_contact) to NumberAction.CreateContact)
                add(stringResource(R.string.menu_add_to_contact) to NumberAction.AddToContact)
            } else {
                add(stringResource(R.string.menu_open_contact) to NumberAction.OpenContact)
            }
            add(stringResource(R.string.menu_block) to NumberAction.Block)
        }
        if (target.calls.isNotEmpty()) {
            add(stringResource(R.string.menu_delete_entry) to NumberAction.DeleteEntry)
            if (!target.isPrivate) add(stringResource(R.string.menu_delete_all) to NumberAction.DeleteAll)
        }
    }
    MenuDialog(
        title = target.contact?.name ?: target.displayNumber.ifBlank { stringResource(R.string.private_number) },
        subtitle = if (target.contact != null) target.displayNumber else null,
        items = items,
        onSelect = { onAction(it); onDismiss() },
        onDismiss = onDismiss,
    )
}

@Composable
fun SimPickerDialog(sims: List<SimAccount>, onPick: (SimAccount) -> Unit, onDismiss: () -> Unit) {
    MenuDialog(
        title = stringResource(R.string.call_with),
        subtitle = null,
        items = sims.map { "${stringResource(R.string.sim_n, it.slot)}  ${it.label}" to it },
        onSelect = { onPick(it); onDismiss() },
        onDismiss = onDismiss,
    )
}

@Composable
fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        shape = RoundedCornerShape(OneUi.cardRadius),
    )
}

@Composable
private fun <T> MenuDialog(
    title: String,
    subtitle: String?,
    items: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(OneUi.cardRadius), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    title,
                    Modifier.padding(horizontal = 24.dp),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.padding(top = 8.dp)) {
                    for ((label, value) in items) {
                        Text(
                            label,
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(value) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

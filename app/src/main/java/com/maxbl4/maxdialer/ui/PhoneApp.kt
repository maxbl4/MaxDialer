package com.maxbl4.maxdialer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxbl4.maxdialer.R
import com.maxbl4.maxdialer.data.SimAccount
import com.maxbl4.maxdialer.telephony.PhoneActions
import com.maxbl4.maxdialer.ui.contacts.ContactsScreen
import com.maxbl4.maxdialer.ui.details.CallDetailsScreen
import com.maxbl4.maxdialer.ui.recents.RecentsActions
import com.maxbl4.maxdialer.ui.recents.RecentsScreen

val requiredPermissions = arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)

val allPermissions = requiredPermissions + arrayOf(
    Manifest.permission.CALL_PHONE,
    Manifest.permission.WRITE_CALL_LOG,
    Manifest.permission.READ_PHONE_STATE,
)

@Composable
fun PhoneApp(vm: PhoneViewModel) {
    val context = LocalContext.current
    fun hasRequired() = requiredPermissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    var granted by remember { mutableStateOf(hasRequired()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasRequired()
        vm.reload()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = hasRequired()
        if (now != granted) vm.reload()
        granted = now
        if (!asked) {
            asked = true
            if (allPermissions.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                launcher.launch(allPermissions)
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.markMissedSeen() }

    // Surface sets the content colour, so plain Text follows the light/dark theme.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (granted) {
            MainContent(vm)
        } else {
            PermissionScreen(
                onGrant = { launcher.launch(allPermissions) },
                onSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )
        }
    }
}

@Composable
private fun MainContent(vm: PhoneViewModel) {
    val context = LocalContext.current
    val data by vm.data.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailsKey by rememberSaveable { mutableStateOf<String?>(null) }
    var dialpadOpen by rememberSaveable { mutableStateOf(true) }
    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    var simPickFor by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAll by remember { mutableStateOf<MenuTarget?>(null) }

    /** Without a default SIM on a dual-SIM phone, ask which one to use. */
    fun call(number: String, sim: SimAccount?) {
        if (sim == null && data.dualSim && data.defaultSim == null) {
            simPickFor = number
        } else {
            PhoneActions.call(context, number, sim?.handle)
            vm.setQuery("")
        }
    }

    fun onAction(target: MenuTarget, action: NumberAction) {
        when (action) {
            is NumberAction.Call -> call(target.number, action.sim)
            NumberAction.Sms -> PhoneActions.sms(context, target.number)
            NumberAction.EditBeforeCall -> {
                vm.setQuery(target.number.filter { it.isDigit() || it in "+*#,;" })
                detailsKey = null
                tab = 0
                dialpadOpen = true
            }
            NumberAction.Copy -> PhoneActions.copy(context, target.number)
            NumberAction.CreateContact -> PhoneActions.createContact(context, target.number)
            NumberAction.AddToContact -> PhoneActions.addToContact(context, target.number)
            NumberAction.OpenContact -> target.contact?.let { PhoneActions.openContact(context, it.contactId, it.lookupKey) }
            NumberAction.Block -> PhoneActions.block(context, target.number)
            NumberAction.DeleteEntry -> vm.deleteCalls(target.calls.map { it.id })
            NumberAction.DeleteAll -> confirmDeleteAll = target
        }
    }

    val key = detailsKey
    BackHandler(enabled = key != null) { detailsKey = null }
    BackHandler(enabled = key == null && tab != 0) { tab = 0 }

    if (key != null) {
        CallDetailsScreen(
            key = key,
            data = data,
            onBack = { detailsKey = null },
            onCall = { call(it, null) },
            onSms = { PhoneActions.sms(context, it) },
            onContact = { t ->
                val c = t.contact
                if (c != null) PhoneActions.openContact(context, c.contactId, c.lookupKey) else PhoneActions.createContact(context, t.number)
            },
            onMenu = { menu = it },
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        )
        // All calls with the number were deleted.
        LaunchedEffect(data, key) { if (data.loaded && data.calls.none { it.key == key }) detailsKey = null }
    } else {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                0 -> RecentsScreen(
                    data = data,
                    query = query,
                    results = results,
                    dialpadOpen = dialpadOpen,
                    onDialpadOpenChange = { dialpadOpen = it },
                    actions = RecentsActions(
                        onType = vm::type,
                        onBackspace = vm::backspace,
                        onClear = { vm.setQuery("") },
                        onPaste = { PhoneActions.pastedNumber(context)?.let(vm::setQuery) },
                        onCall = ::call,
                        onSms = { PhoneActions.sms(context, it) },
                        onAddContact = { PhoneActions.createContact(context, it) },
                        onOpenDetails = { detailsKey = it },
                        onMenu = { menu = it },
                    ),
                    modifier = Modifier.weight(1f),
                )
                else -> ContactsScreen(
                    contacts = data.contacts,
                    loaded = data.loaded,
                    onOpen = { PhoneActions.openContact(context, it.contactId, it.lookupKey) },
                    onCreate = { PhoneActions.createContact(context, "") },
                    modifier = Modifier.weight(1f),
                )
            }
            BottomTabs(tab) { tab = it }
        }
    }

    menu?.let { target ->
        NumberMenuDialog(target, data.sims, onAction = { onAction(target, it) }, onDismiss = { menu = null })
    }
    simPickFor?.let { number ->
        SimPickerDialog(data.sims, onPick = { call(number, it) }, onDismiss = { simPickFor = null })
    }
    confirmDeleteAll?.let { target ->
        ConfirmDeleteDialog(
            text = stringResource(R.string.delete_all_confirm, target.contact?.name ?: target.displayNumber),
            onConfirm = { vm.deleteCalls(data.calls.filter { it.key == target.calls.first().key }.map { it.id }) },
            onDismiss = { confirmDeleteAll = null },
        )
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Icons.Filled.History to stringResource(R.string.tab_recents),
        Icons.Filled.Person to stringResource(R.string.tab_contacts),
    )
    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp)) {
        tabs.forEachIndexed { i, (icon, label) -> Tab(icon, label, i == selected, Modifier.weight(1f)) { onSelect(i) } }
    }
}

@Composable
private fun Tab(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.fillMaxSize().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color)
        Text(label, color = color, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.permissions_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.permissions_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text(stringResource(R.string.permissions_grant)) }
        Text(
            stringResource(R.string.permissions_settings),
            Modifier.clickable(onClick = onSettings).padding(12.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

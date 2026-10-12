package com.maxbl4.maxdialer.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.maxbl4.maxdialer.search.SearchItem
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Reads and changes the system call log, contacts and SIM cards. */
class PhoneRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val telecom = context.getSystemService(TelecomManager::class.java)
    private val telephony = context.getSystemService(TelephonyManager::class.java)

    fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun load(): PhoneData {
        val iso = countryIso()
        val contacts = if (granted(Manifest.permission.READ_CONTACTS)) loadContacts() else emptyList()
        val contactsByKey = HashMap<String, ContactPhone>()
        for (c in contacts) contactsByKey.putIfAbsent(c.key, c)
        val calls = if (granted(Manifest.permission.READ_CALL_LOG)) loadCalls() else emptyList()
        val sims = loadSims()
        return PhoneData(
            loaded = true,
            contacts = contacts,
            contactsByKey = contactsByKey,
            calls = calls,
            recents = groupCalls(calls, contactsByKey),
            favorites = favorites(contacts, calls),
            sims = sims,
            defaultSim = defaultSim(),
            countryIso = iso,
            searchItems = buildSearchItems(contacts, calls, iso),
        )
    }

    private fun loadContacts(): List<ContactPhone> {
        val projection = arrayOf(
            Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER,
            Phone.TYPE, Phone.LABEL, Phone.PHOTO_THUMBNAIL_URI, Phone.STARRED,
        )
        val result = ArrayList<ContactPhone>()
        resolver.query(Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(3) ?: continue
                result += ContactPhone(
                    contactId = c.getLong(0),
                    lookupKey = c.getString(1) ?: "",
                    name = c.getString(2) ?: number,
                    number = number,
                    type = c.getInt(4),
                    label = c.getString(5),
                    photoUri = c.getString(6),
                    starred = c.getInt(7) != 0,
                )
            }
        }
        result.sortWith(compareBy<ContactPhone>({ scriptOrder(it.name) }, { it.name.lowercase() }))
        return result
    }

    private fun loadCalls(): List<CallEntry> {
        val uri = CallLog.Calls.CONTENT_URI.buildUpon()
            .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, CALL_LIMIT.toString())
            .build()
        val projection = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.TYPE,
            CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.PHONE_ACCOUNT_ID, CallLog.Calls.NEW,
            CallLog.Calls.CACHED_NAME,
        )
        val result = ArrayList<CallEntry>()
        resolver.query(uri, projection, null, null, "${CallLog.Calls.DATE} DESC")?.use { c ->
            while (c.moveToNext()) {
                result += CallEntry(
                    id = c.getLong(0),
                    number = c.getString(1) ?: "",
                    presentation = c.getInt(2),
                    type = c.getInt(3),
                    date = c.getLong(4),
                    duration = c.getLong(5),
                    accountId = c.getString(6),
                    isNew = c.getInt(7) != 0,
                    cachedName = c.getString(8),
                )
            }
        }
        return result
    }

    @SuppressLint("MissingPermission")
    private fun loadSims(): List<SimAccount> {
        if (!granted(Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val subscriptions = context.getSystemService(SubscriptionManager::class.java)
        return telecom.callCapablePhoneAccounts.mapIndexed { i, handle ->
            val info = subscriptions.getActiveSubscriptionInfo(telephony.getSubscriptionId(handle))
            SimAccount(
                handle = handle,
                slot = info?.simSlotIndex?.plus(1) ?: (i + 1),
                label = info?.displayName?.toString() ?: telecom.getPhoneAccount(handle)?.label?.toString() ?: "",
            )
        }.sortedBy { it.slot }
    }

    @SuppressLint("MissingPermission")
    private fun defaultSim(): PhoneAccountHandle? =
        if (granted(Manifest.permission.READ_PHONE_STATE)) telecom.getDefaultOutgoingPhoneAccount("tel") else null

    private fun countryIso(): String =
        telephony.networkCountryIso.ifEmpty { telephony.simCountryIso }.ifEmpty { Locale.getDefault().country }
            .uppercase()

    fun deleteCalls(ids: Collection<Long>) = writeCallLog {
        ids.chunked(500).forEach { chunk ->
            resolver.delete(CallLog.Calls.CONTENT_URI, "${CallLog.Calls._ID} IN (${chunk.joinToString(",")})", null)
        }
    }

    /** Missed calls stop being highlighted once the user has seen them. */
    fun markMissedSeen() = writeCallLog {
        val values = ContentValues().apply {
            put(CallLog.Calls.NEW, 0)
            put(CallLog.Calls.IS_READ, 1)
        }
        resolver.update(
            CallLog.Calls.CONTENT_URI, values,
            "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.NEW} = 1", null,
        )
    }

    /** Returns false when the app may not write contacts. */
    fun setStarred(contactId: Long, starred: Boolean): Boolean {
        if (!granted(Manifest.permission.WRITE_CONTACTS)) return false
        return try {
            val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
            resolver.update(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId), values, null, null)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts write denied", e)
            false
        }
    }

    private fun writeCallLog(block: () -> Unit) {
        if (!granted(Manifest.permission.WRITE_CALL_LOG)) return
        try {
            block()
        } catch (e: SecurityException) {
            Log.w(TAG, "Call log write denied", e)
        }
    }

    /**
     * Calls [listener] on the main thread when the call log or contacts change.
     * Watching needs the read permission, so call [update] again once permissions are granted.
     */
    inner class Watcher(listener: () -> Unit) {
        private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = listener()
        }
        private val watched = HashSet<Uri>()

        fun update() {
            watch(CallLog.Calls.CONTENT_URI, Manifest.permission.READ_CALL_LOG)
            watch(ContactsContract.Contacts.CONTENT_URI, Manifest.permission.READ_CONTACTS)
        }

        private fun watch(uri: Uri, permission: String) {
            if (uri in watched || !granted(permission)) return
            resolver.registerContentObserver(uri, true, observer)
            watched += uri
        }

        fun stop() = resolver.unregisterContentObserver(observer)
    }

    companion object {
        private const val TAG = "PhoneRepository"
        private const val CALL_LIMIT = 2000

        fun formatNumber(number: String, countryIso: String): String =
            PhoneNumberUtils.formatNumber(number, countryIso) ?: number
    }
}

/** Russian names first, then Latin, then the rest. */
fun scriptOrder(name: String): Int {
    val c = name.firstOrNull { it.isLetter() } ?: return 3
    return when (Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.CYRILLIC -> 0
        Character.UnicodeScript.LATIN -> 1
        else -> 2
    }
}

/** One entry per starred contact: the number called last, or the first one. Keeps the contacts order. */
internal fun favorites(contacts: List<ContactPhone>, calls: List<CallEntry>): List<ContactPhone> {
    val lastCall = HashMap<String, Long>()
    for (call in calls) if (!call.isPrivate) lastCall.putIfAbsent(call.key, call.date)
    return contacts.filter { it.starred }
        .groupBy { it.contactId }
        .values
        .map { phones -> phones.maxBy { lastCall[it.key] ?: 0L } }
}

internal fun groupCalls(calls: List<CallEntry>, contactsByKey: Map<String, ContactPhone>): List<RecentGroup> {
    val zone = ZoneId.systemDefault()
    fun day(call: CallEntry) = Instant.ofEpochMilli(call.date).atZone(zone).toLocalDate()

    val groups = ArrayList<RecentGroup>()
    var current = ArrayList<CallEntry>()
    fun flush() {
        if (current.isEmpty()) return
        groups += RecentGroup(current, contactsByKey[current.first().key].takeUnless { current.first().isPrivate })
        current = ArrayList()
    }
    for (call in calls) {
        val prev = current.lastOrNull()
        if (prev != null && (prev.key != call.key || day(prev) != day(call))) flush()
        current += call
    }
    flush()
    return groups
}

private fun buildSearchItems(
    contacts: List<ContactPhone>,
    calls: List<CallEntry>,
    iso: String,
): List<SearchItem<SearchTarget>> {
    class Stat(var lastCall: Long = 0, var count: Int = 0)

    val stats = HashMap<String, Stat>()
    for (call in calls) {
        if (call.isPrivate) continue
        val stat = stats.getOrPut(call.key) { Stat() }
        stat.lastCall = maxOf(stat.lastCall, call.date)
        stat.count++
    }

    val items = ArrayList<SearchItem<SearchTarget>>()
    val seen = HashSet<String>()
    for (p in contacts) {
        if (!seen.add("${p.contactId}/${p.key}")) continue
        val stat = stats[p.key]
        items += SearchItem(
            SearchTarget(p.number, p), p.name, PhoneRepository.formatNumber(p.number, iso),
            stat?.lastCall ?: 0, stat?.count ?: 0,
        )
    }
    val contactKeys = contacts.mapTo(HashSet()) { it.key }
    for (call in calls) {
        if (call.isPrivate || call.key in contactKeys || !seen.add(call.key)) continue
        val stat = stats.getValue(call.key)
        items += SearchItem(
            SearchTarget(call.number, null), null, PhoneRepository.formatNumber(call.number, iso),
            stat.lastCall, stat.count,
        )
    }
    return items
}

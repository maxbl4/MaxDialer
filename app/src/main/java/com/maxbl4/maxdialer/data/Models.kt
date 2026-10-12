package com.maxbl4.maxdialer.data

import android.provider.CallLog
import android.telecom.PhoneAccountHandle
import com.maxbl4.maxdialer.search.SearchItem
import com.maxbl4.maxdialer.search.numberKey

/** One phone number of a contact. */
data class ContactPhone(
    val contactId: Long,
    val lookupKey: String,
    val name: String,
    val number: String,
    val type: Int,
    val label: String?,
    val photoUri: String?,
) {
    val key = numberKey(number)
}

data class CallEntry(
    val id: Long,
    val number: String,
    val presentation: Int,
    val type: Int,
    val date: Long,
    val duration: Long,
    val accountId: String?,
    val isNew: Boolean,
    val cachedName: String?,
) {
    val isPrivate = number.isBlank() || presentation != CallLog.Calls.PRESENTATION_ALLOWED

    /** Joins calls with the same number; all hidden numbers share one key. */
    val key = if (isPrivate) PRIVATE_KEY else numberKey(number)

    val isMissed = type == CallLog.Calls.MISSED_TYPE

    companion object {
        const val PRIVATE_KEY = "private"
    }
}

/** Consecutive calls with the same number on the same day, newest first. */
data class RecentGroup(val calls: List<CallEntry>, val contact: ContactPhone?) {
    val latest get() = calls.first()
    val key get() = latest.key
}

data class SimAccount(val handle: PhoneAccountHandle, val slot: Int, val label: String)

/** What a dialpad search result points at. */
data class SearchTarget(val number: String, val contact: ContactPhone?)

data class PhoneData(
    val loaded: Boolean = false,
    val contacts: List<ContactPhone> = emptyList(),
    val contactsByKey: Map<String, ContactPhone> = emptyMap(),
    /** Newest first. */
    val calls: List<CallEntry> = emptyList(),
    val recents: List<RecentGroup> = emptyList(),
    val sims: List<SimAccount> = emptyList(),
    val defaultSim: PhoneAccountHandle? = null,
    val countryIso: String = "",
    val searchItems: List<SearchItem<SearchTarget>> = emptyList(),
) {
    val dualSim get() = sims.size > 1

    /** SIM slot (1-based) for a call log account id, or null on single-SIM phones. */
    fun simSlot(accountId: String?): Int? =
        if (!dualSim || accountId == null) null else sims.firstOrNull { it.handle.id == accountId }?.slot
}

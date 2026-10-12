package com.maxbl4.maxdialer.ui.recents

import android.provider.CallLog
import com.maxbl4.maxdialer.data.CallEntry
import com.maxbl4.maxdialer.data.ContactPhone
import com.maxbl4.maxdialer.data.RecentGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class LogSearchTest {
    private var nextId = 0L

    private fun group(number: String, contactName: String? = null, cachedName: String? = null, private: Boolean = false): RecentGroup {
        val id = ++nextId
        val call = CallEntry(
            id, number,
            if (private) CallLog.Calls.PRESENTATION_RESTRICTED else CallLog.Calls.PRESENTATION_ALLOWED,
            CallLog.Calls.INCOMING_TYPE, id, 0, null, false, cachedName,
        )
        val contact = contactName?.let { ContactPhone(id, "", it, number, 0, null, null) }
        return RecentGroup(listOf(call), contact)
    }

    private val ivan = group("+7 916 123-45-67", contactName = "Иван Петров")
    private val shop = group("8 495 000-11-22", cachedName = "Avito")
    private val unknown = group("+7 903 555-12-34")
    private val hidden = group("", private = true)
    private val log = listOf(ivan, shop, unknown, hidden)

    private fun search(query: String) = filterRecents(log, query).map { it.latest.number }

    @Test
    fun `empty query keeps everything`() {
        assertEquals(log.map { it.latest.number }, search(" "))
    }

    @Test
    fun `contact and cached names with layout fix and transliteration`() {
        assertEquals(listOf(ivan.latest.number), search("петр"))
        assertEquals(listOf(ivan.latest.number), search("ivan"))
        assertEquals(listOf(ivan.latest.number), search("bdfy"))
        assertEquals(listOf(shop.latest.number), search("авито"))
    }

    @Test
    fun `number digits with 8 and +7 alike keep log order`() {
        assertEquals(listOf(unknown.latest.number), search("8903"))
        assertEquals(listOf(ivan.latest.number, unknown.latest.number), search("+7 9"))
    }
}

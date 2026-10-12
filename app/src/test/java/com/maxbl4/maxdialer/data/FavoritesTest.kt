package com.maxbl4.maxdialer.data

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesTest {
    private fun phone(id: Long, name: String, number: String, starred: Boolean = true) =
        ContactPhone(id, "", name, number, 0, null, null, starred)

    private fun call(number: String, date: Long) =
        CallEntry(date, number, CallLog.Calls.PRESENTATION_ALLOWED, CallLog.Calls.OUTGOING_TYPE, date, 0, null, false, null)

    @Test
    fun `one entry per starred contact with the number called last`() {
        val contacts = listOf(
            phone(1, "Анна", "+7 900 000-00-01"),
            phone(1, "Анна", "+7 900 000-00-02"),
            phone(2, "Борис", "+7 900 000-00-03", starred = false),
            phone(3, "Вера", "+7 900 000-00-04"),
        )
        val calls = listOf(call("89000000002", 20), call("89000000001", 10))
        assertEquals(
            listOf("+7 900 000-00-02", "+7 900 000-00-04"),
            favorites(contacts, calls).map { it.number },
        )
    }
}

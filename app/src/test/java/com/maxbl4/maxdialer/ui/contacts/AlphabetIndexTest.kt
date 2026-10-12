package com.maxbl4.maxdialer.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetIndexTest {
    @Test
    fun `all letters when they fit`() {
        assertEquals("АБВ".toList(), visibleLabels("АБВ".toList(), 5))
    }

    @Test
    fun `first and last letters kept with dots between when they do not fit`() {
        val letters = "АБВГДЕЖЗИК".toList()
        val labels = visibleLabels(letters, 5)
        assertEquals("А•Д•К".toList(), labels)
    }
}

package com.maxbl4.maxdialer.ui.recents

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DialEditingTest {
    private fun v(text: String, cursor: Int = text.length) = TextFieldValue(text, TextRange(cursor))

    @Test
    fun `insert at cursor and replace selection`() {
        assertEquals(v("8916", 2), v("816", 1).insert("9"))
        assertEquals(v("8016", 2), TextFieldValue("8916", TextRange(1, 2)).insert("0"))
        assertEquals(v("89165"), v("8916").insert("5"))
    }

    @Test
    fun `delete before cursor or the selection`() {
        assertEquals(v("816", 1), v("8916", 2).deleteBackward())
        assertEquals(v("8916", 0), v("8916", 0).deleteBackward())
        assertEquals(v("86", 1), TextFieldValue("8916", TextRange(1, 3)).deleteBackward())
    }

    @Test
    fun `pasted separators are dropped and the cursor stays on the same digits`() {
        assertEquals(v("89161", 5), v("8 (916) 1", 9).sanitized())
        assertEquals(v("+7916", 2), v("+7 916", 2).sanitized())
    }

    @Test
    fun `cursor maps between typed and formatted number`() {
        val m = FormattedOffsets("89161234567", "8 916 123-45-67")
        assertEquals(0, m.originalToTransformed(0))
        assertEquals(1, m.originalToTransformed(1))
        assertEquals(5, m.originalToTransformed(4))
        assertEquals(15, m.originalToTransformed(11))
        assertEquals(1, m.transformedToOriginal(2))
        assertEquals(4, m.transformedToOriginal(6))
        assertEquals(11, m.transformedToOriginal(15))
    }
}

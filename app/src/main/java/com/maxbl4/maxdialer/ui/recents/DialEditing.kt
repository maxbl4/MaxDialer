package com.maxbl4.maxdialer.ui.recents

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

/** Characters a dialled number may contain: digits, +, *, #, pause and wait. */
internal fun isDialChar(c: Char) = c in '0'..'9' || c in "+*#,;"

/** Replaces the selection (or inserts at the cursor) and puts the cursor after the new text. */
internal fun TextFieldValue.insert(s: String): TextFieldValue {
    val start = selection.min
    val text = text.substring(0, start) + s + text.substring(selection.max)
    return TextFieldValue(text, TextRange(start + s.length))
}

/** Deletes the selection, or the character before the cursor. */
internal fun TextFieldValue.deleteBackward(): TextFieldValue {
    if (!selection.collapsed) return insert("")
    val at = selection.start
    if (at == 0) return this
    return TextFieldValue(text.removeRange(at - 1, at), TextRange(at - 1))
}

/** Drops characters that cannot be dialled (from paste), keeping the cursor on the same characters. */
internal fun TextFieldValue.sanitized(): TextFieldValue {
    if (text.all(::isDialChar)) return this
    fun kept(end: Int) = text.substring(0, end).count(::isDialChar)
    return TextFieldValue(text.filter(::isDialChar), TextRange(kept(selection.start), kept(selection.end)))
}

/**
 * Cursor mapping between the typed number and its formatted form, which has the same characters
 * in the same order plus separators («89161234567» → «8 916 123-45-67»).
 */
internal class FormattedOffsets(original: String, formatted: String) : OffsetMapping {
    private val toFormatted = IntArray(original.length + 1)
    private val toOriginal = IntArray(formatted.length + 1)

    init {
        var j = 0
        for (i in original.indices) {
            while (j < formatted.length && formatted[j] != original[i]) j++
            require(j < formatted.length) { "«$formatted» is not «$original» with separators" }
            toFormatted[i + 1] = j + 1
            j++
        }
        var i = 0
        for (k in 0..formatted.length) {
            while (i < original.length && toFormatted[i + 1] <= k) i++
            toOriginal[k] = i
        }
    }

    override fun originalToTransformed(offset: Int) = toFormatted[offset.coerceIn(0, toFormatted.size - 1)]

    override fun transformedToOriginal(offset: Int) = toOriginal[offset.coerceIn(0, toOriginal.size - 1)]
}

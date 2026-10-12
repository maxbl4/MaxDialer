package com.maxbl4.maxdialer.search

import java.text.Normalizer

/** Latin and Russian phone keypad letters, indexed by digit. */
object T9 {
    val latinLetters = listOf("", "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ")
    val cyrillicLetters = listOf("", "", "АБВГ", "ДЕЖЗ", "ИЙКЛ", "МНОП", "РСТУ", "ФХЦЧ", "ШЩЪЫ", "ЬЭЮЯ")

    private val table: Map<Char, Char> = buildMap {
        for (digit in 2..9) {
            for (c in latinLetters[digit] + cyrillicLetters[digit]) put(c.lowercaseChar(), '0' + digit)
        }
    }

    /** Digit for a letter or digit, or null for separators. Diacritics are ignored: ё → е, é → e. */
    fun digitOf(c: Char): Char? {
        if (c in '0'..'9') return c
        val lower = c.lowercaseChar()
        table[lower]?.let { return it }
        val base = Normalizer.normalize(lower.toString(), Normalizer.Form.NFD).firstOrNull() ?: return null
        return table[base]
    }

    /** A word of [digits]; positions[i] is the index in the source text of the char that produced digits[i]. */
    class Token(val digits: String, val positions: IntArray)

    fun tokenize(text: String): List<Token> {
        val tokens = ArrayList<Token>()
        val digits = StringBuilder()
        val positions = ArrayList<Int>()
        fun flush() {
            if (digits.isNotEmpty()) tokens += Token(digits.toString(), positions.toIntArray())
            digits.clear()
            positions.clear()
        }
        text.forEachIndexed { i, c ->
            val d = digitOf(c)
            if (d == null) flush() else {
                digits.append(d)
                positions += i
            }
        }
        flush()
        return tokens
    }
}

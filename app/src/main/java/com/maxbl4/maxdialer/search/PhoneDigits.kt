package com.maxbl4.maxdialer.search

/** Digits of a phone number; positions[i] is the index in the source text of digits[i]. */
class NumberDigits(val digits: String, val positions: IntArray) {
    companion object {
        fun of(number: String): NumberDigits {
            val digits = StringBuilder()
            val positions = ArrayList<Int>()
            number.forEachIndexed { i, c ->
                if (c in '0'..'9') {
                    digits.append(c)
                    positions += i
                }
            }
            return NumberDigits(digits.toString(), positions.toIntArray())
        }
    }
}

/**
 * Key that joins the same number written differently (+7 916…, 8 916…, 916…):
 * the last 10 digits, or all digits for short numbers.
 */
fun numberKey(number: String): String {
    val digits = number.filter { it in '0'..'9' }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

/** A way of writing [NumberDigits.digits]; variant index i is digit index i + offset of the original. */
internal class NumberVariant(val digits: String, val offset: Int)

/**
 * The number as people may type it. Russian numbers are dialled as +7 or 8 interchangeably,
 * and sometimes without the prefix at all.
 */
internal fun numberVariants(digits: String): List<NumberVariant> = buildList {
    add(NumberVariant(digits, 0))
    if (digits.length == 11 && (digits[0] == '7' || digits[0] == '8')) {
        add(NumberVariant((if (digits[0] == '7') "8" else "7") + digits.substring(1), 0))
        add(NumberVariant(digits.substring(1), 1))
    } else if (digits.length == 10) {
        add(NumberVariant("8$digits", -1))
        add(NumberVariant("7$digits", -1))
    }
}

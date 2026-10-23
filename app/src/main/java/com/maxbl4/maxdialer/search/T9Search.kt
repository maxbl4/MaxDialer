package com.maxbl4.maxdialer.search

/** Something the dialpad can find: a contact's number or a number from the call log. */
class SearchItem<T>(
    val value: T,
    /** Contact name, or null for a number that is not in contacts. */
    val name: String?,
    /** Number as displayed; highlights index into this string. */
    val number: String,
    /** Time of the last call with this number, 0 if none. */
    val lastCall: Long,
    val callCount: Int,
    /** Family name from the contact, when it has one; it must be part of [name]. */
    val surname: String? = null,
) {
    internal val tokens = name?.let(T9::tokenize).orEmpty()

    /** Index in [tokens] of the first word of the surname, or -1. */
    internal val surnameToken: Int = run {
        if (name == null || surname.isNullOrBlank()) return@run -1
        val at = name.indexOf(surname.trim(), ignoreCase = true)
        if (at < 0) -1 else tokens.indexOfFirst { it.positions[0] >= at }
    }
    internal val numberDigits = NumberDigits.of(number)
    internal val variants = numberVariants(numberDigits.digits)
}

class SearchHit<T>(
    val item: SearchItem<T>,
    val score: Int,
    /** Char ranges of [SearchItem.name] that matched. */
    val nameHighlights: List<IntRange>,
    /** Char range of [SearchItem.number] that matched. */
    val numberHighlight: IntRange?,
)

/**
 * Dialpad search over names (T9, Latin and Cyrillic at once) and numbers.
 * Results come in three tiers: the number being typed, word starts of names, then weak matches.
 * Within a tier, people called recently go first, then the more exact match: surname before first name.
 */
object T9Search {
    const val EXACT_NUMBER = 110
    /** The query is the start of a number and long enough that the user is surely typing a number. */
    const val NUMBER_PREFIX_LONG = 100
    /** The query is the start of the surname: «Изо» → Димон Изотов. */
    const val SURNAME_START = 92
    /** The query is the start of the first word of the name. */
    const val NAME_START = 90
    /** The query spans the starts of several words from the first one: «ИП», «ИванП» → Иван Петров. */
    const val WORDS_FROM_START = 85
    /** The query is the start of another word of the name. */
    const val WORD_START = 80
    /** The query spans the starts of several words, not from the first one. */
    const val WORDS = 75
    const val NUMBER_PREFIX_MEDIUM = 65
    const val NUMBER_PREFIX_SHORT = 50
    /** The query is inside a word of the name. */
    const val NAME_INFIX = 40
    /** The query is inside the number. */
    const val NUMBER_INFIX = 30

    const val LONG_NUMBER_PREFIX = 5
    private const val MIN_NUMBER_INFIX = 3
    private const val MIN_NAME_INFIX = 2

    /**
     * 0: clearly a number being typed; 1: starts of name words and short number prefixes;
     * 2: matches inside words or numbers.
     */
    private fun tier(score: Int) = when {
        score >= NUMBER_PREFIX_LONG -> 0
        score >= NUMBER_PREFIX_MEDIUM -> 1
        else -> 2
    }

    fun <T> search(items: List<SearchItem<T>>, query: String, limit: Int = 200): List<SearchHit<T>> {
        val numberQuery = query.removePrefix("+")
        if (numberQuery.isEmpty() || numberQuery.any { it !in '0'..'9' }) return emptyList()
        val searchNames = !query.startsWith("+")

        val hits = ArrayList<SearchHit<T>>()
        for (item in items) {
            val name = if (searchNames) matchName(item.tokens, item.surnameToken, numberQuery) else null
            val number = matchNumber(item, numberQuery)
            val score = maxOf(name?.score ?: 0, number?.score ?: 0)
            if (score == 0) continue
            hits += SearchHit(item, score, name?.highlights.orEmpty(), number?.highlight)
        }
        hits.sortWith(
            compareBy<SearchHit<T>> { tier(it.score) }
                .thenByDescending { it.item.lastCall }
                .thenByDescending { it.score }
                .thenByDescending { it.item.callCount }
                .thenBy(nullsLast()) { it.item.name?.lowercase() }
                .thenBy { it.item.number },
        )
        return if (hits.size > limit) hits.subList(0, limit) else hits
    }

    private class NameMatch(val score: Int, val highlights: List<IntRange>)

    private class NumberMatch(val score: Int, val highlight: IntRange)

    /** A non-empty prefix of length [length] of token [token]. */
    private class Piece(val token: Int, val length: Int)

    private fun matchName(tokens: List<T9.Token>, surnameToken: Int, query: String): NameMatch? {
        var best: NameMatch? = null
        for (start in tokens.indices) {
            val pieces = split(tokens, start, query, 0) ?: continue
            val score = when {
                pieces.size == 1 && start == surnameToken -> SURNAME_START
                pieces.size == 1 && start == 0 -> NAME_START
                pieces.size == 1 -> WORD_START
                start == 0 -> WORDS_FROM_START
                else -> WORDS
            }
            if (best == null || score > best.score) {
                best = NameMatch(score, pieces.map { tokens[it.token].range(0, it.length) })
            }
        }
        if (best != null || query.length < MIN_NAME_INFIX) return best
        for (token in tokens) {
            val at = token.digits.indexOf(query, startIndex = 1)
            if (at > 0) return NameMatch(NAME_INFIX, listOf(token.range(at, query.length)))
        }
        return null
    }

    /**
     * Splits query[from..] into prefixes of consecutive tokens starting at [token].
     * Longer prefixes are tried first, so a match within one word wins over a split across words.
     */
    private fun split(tokens: List<T9.Token>, token: Int, query: String, from: Int): List<Piece>? {
        if (token >= tokens.size) return null
        val digits = tokens[token].digits
        for (length in minOf(digits.length, query.length - from) downTo 1) {
            if (!digits.regionMatches(0, query, from, length)) continue
            if (from + length == query.length) return listOf(Piece(token, length))
            val rest = split(tokens, token + 1, query, from + length) ?: continue
            return listOf(Piece(token, length)) + rest
        }
        return null
    }

    private fun T9.Token.range(start: Int, length: Int) = positions[start]..positions[start + length - 1]

    private fun matchNumber(item: SearchItem<*>, query: String): NumberMatch? {
        var best: NumberMatch? = null
        for (variant in item.variants) {
            val at: Int
            val score = when {
                variant.digits == query && query.length >= MIN_NUMBER_INFIX -> { at = 0; EXACT_NUMBER }
                variant.digits.startsWith(query) -> {
                    at = 0
                    when {
                        query.length >= LONG_NUMBER_PREFIX -> NUMBER_PREFIX_LONG
                        query.length >= MIN_NUMBER_INFIX -> NUMBER_PREFIX_MEDIUM
                        else -> NUMBER_PREFIX_SHORT
                    }
                }
                query.length >= MIN_NUMBER_INFIX -> {
                    at = variant.digits.indexOf(query)
                    if (at < 0) continue
                    NUMBER_INFIX
                }
                else -> continue
            }
            if (best == null || score > best.score) {
                best = NumberMatch(score, item.numberDigits.range(at + variant.offset, query.length))
            }
        }
        return best
    }

    private fun NumberDigits.range(start: Int, length: Int): IntRange {
        val first = start.coerceIn(0, digits.length - 1)
        val last = (start + length - 1).coerceIn(first, digits.length - 1)
        return positions[first]..positions[last]
    }
}

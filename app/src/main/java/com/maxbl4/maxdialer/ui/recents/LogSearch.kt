package com.maxbl4.maxdialer.ui.recents

import com.maxbl4.maxdialer.data.RecentGroup
import com.maxbl4.maxdialer.search.TextSearch
import com.maxbl4.maxdialer.search.numberVariants

/**
 * Call log entries matching [query], in log order: by contact or cached name (with layout fix and
 * transliteration), or, for a number-like query, by digits of the number with +7 and 8 alike.
 */
internal fun filterRecents(recents: List<RecentGroup>, query: String): List<RecentGroup> {
    if (query.isBlank()) return recents
    val nameMatches = TextSearch.matcher(query)
    val digits = query.filter { it.isDigit() }
    val numberLike = digits.isNotEmpty() && query.all { it.isDigit() || it in "+-() " }
    return recents.filter { group ->
        val call = group.latest
        if (call.isPrivate) return@filter false
        val name = group.contact?.name ?: call.cachedName
        (name != null && nameMatches(name)) ||
            (numberLike && numberVariants(call.number.filter { it.isDigit() }).any { digits in it.digits })
    }
}

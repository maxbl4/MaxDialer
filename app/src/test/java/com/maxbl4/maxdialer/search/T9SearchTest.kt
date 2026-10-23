package com.maxbl4.maxdialer.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9SearchTest {
    private fun item(name: String?, number: String, lastCall: Long = 0, count: Int = 0) =
        SearchItem(name ?: number, name, number, lastCall, count)

    private fun search(query: String, vararg items: SearchItem<String>) =
        T9Search.search(items.toList(), query).map { it.item.value }

    @Test
    fun `cyrillic and latin letters map to the same keys`() {
        assertEquals(setOf("Иван", "Ivan"), search("4", item("Иван", "1"), item("Ivan", "2")).toSet())
        assertEquals(listOf("Иван"), search("4225", item("Иван", "1"), item("Ivan", "2")))
        assertEquals(listOf("Ivan"), search("4826", item("Иван", "1"), item("Ivan", "2")))
    }

    @Test
    fun `yo and short i match e and i`() {
        assertEquals(listOf("Алёна"), search("24352", item("Алёна", "1")))
        assertEquals(listOf("Йога"), search("4", item("Йога", "1")))
    }

    @Test
    fun `initials match`() {
        assertEquals(listOf("Иван Петров"), search("45", item("Иван Петров", "1")))
        assertEquals(listOf("Иван Петров"), search("422553", item("Иван Петров", "1")))
    }

    @Test
    fun `start of name ranks above other words and infix`() {
        val infix = item("Абв Аиван", "1")
        val otherWord = item("Петр Иванов", "2")
        val start = item("Иванов Петр", "3")
        assertEquals(
            listOf("Иванов Петр", "Петр Иванов", "Абв Аиван"),
            search("4225", infix, otherWord, start),
        )
    }

    @Test
    fun `someone called recently ranks above never called even when the match is on the surname`() {
        val neverCalled = item("Hell-Hound", "1")
        val calledYesterday = item("Димон Изотов", "2", lastCall = 100)
        val calledLongAgo = item("Кентавр Саша", "3", lastCall = 10)
        assertEquals(
            listOf("Димон Изотов", "Кентавр Саша", "Hell-Hound"),
            search("435", neverCalled, calledYesterday, calledLongAgo),
        )
    }

    @Test
    fun `surname start ranks above first name start`() {
        val firstName = item("Изольда Петрова", "1")
        val surname = SearchItem("Димон Изотов", "Димон Изотов", "2", 0, 0, surname = "Изотов")
        val surnameFirst = SearchItem("Изотов Дмитрий", "Изотов Дмитрий", "3", 0, 0, surname = "Изотов")
        assertEquals(
            setOf("Димон Изотов", "Изотов Дмитрий"),
            search("435", firstName, surname, surnameFirst).take(2).toSet(),
        )
        assertEquals("Изольда Петрова", search("435", firstName, surname, surnameFirst).last())
    }

    @Test
    fun `matches inside a word stay below word starts despite call history`() {
        val inside = item("Абв Аиван", "1", lastCall = 100)
        val wordStart = item("Иван", "2")
        assertEquals(listOf("Иван", "Абв Аиван"), search("4225", inside, wordStart))
    }

    @Test
    fun `long number prefix ranks above names`() {
        val name = item("Tyano", "+7 999 000-00-00")
        val number = item("Яна", "8 926 612-34-56")
        assertEquals(listOf("Яна", "Tyano"), search("89266", name, number))
    }

    @Test
    fun `short number prefix ranks below names`() {
        val name = item("Tanya", "+7 999 000-00-00")
        val number = item("Яна", "82 00")
        assertEquals(listOf("Tanya", "Яна"), search("82", name, number))
    }

    @Test
    fun `plus 7 and 8 are the same prefix`() {
        assertEquals(listOf("+7 916 123-45-67"), search("89161", item(null, "+7 916 123-45-67")))
        assertEquals(listOf("8 916 123-45-67"), search("79161", item(null, "8 916 123-45-67")))
        assertEquals(listOf("8 916 123-45-67"), search("91612", item(null, "8 916 123-45-67")))
    }

    @Test
    fun `plus searches numbers only`() {
        assertEquals(listOf("Б"), search("+7", item("Ру", "1"), item("Б", "+7 1")))
    }

    @Test
    fun `codes are not searched`() {
        assertTrue(search("*100#", item("Баланс", "*100#")).isEmpty())
    }

    @Test
    fun `recent calls go first within the same rank`() {
        assertEquals(
            listOf("Иванов", "Ивлев", "Игорь"),
            search("4", item("Игорь", "1"), item("Ивлев", "2", lastCall = 10), item("Иванов", "3", lastCall = 20)),
        )
    }

    @Test
    fun `highlights point into display strings`() {
        val hit = T9Search.search(listOf(item("Иван Петров", "+7 916 123-45-67")), "45").single()
        assertEquals(listOf(0..0, 5..5), hit.nameHighlights)

        val numberHit = T9Search.search(listOf(item(null, "+7 916 123-45-67")), "89161").single()
        assertEquals(1..7, numberHit.numberHighlight)
    }
}

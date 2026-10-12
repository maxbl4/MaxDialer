package com.maxbl4.maxdialer.ui.contacts

import com.maxbl4.maxdialer.data.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSearchTest {
    private var nextId = 0L
    private fun contact(name: String, number: String, id: Long = ++nextId) =
        ContactPhone(id, "", name, number, 0, null, null)

    private val ivan = contact("Иван Петров", "+7 916 123-45-67")
    private val ivanWork = ivan.copy(number = "+7 495 000-11-22")
    private val anna = contact("Anna Smith", "8 903 555-12-34")
    private val all = listOf(ivan, ivanWork, anna)

    private fun search(query: String) = searchContacts(all, query).map { it.name }

    @Test
    fun `one entry per contact`() {
        assertEquals(listOf("Иван Петров"), search("пет"))
    }

    @Test
    fun `wrong layout and transliteration`() {
        assertEquals(listOf("Иван Петров"), search("bdfy"))
        assertEquals(listOf("Anna Smith"), search("анна"))
    }

    @Test
    fun `number digits with 8 and +7`() {
        assertEquals(listOf("Иван Петров"), search("000-11"))
        assertEquals(listOf("Anna Smith"), search("7903555"))
        assertEquals(listOf("Иван Петров"), search("8916"))
    }
}

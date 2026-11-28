package com.warwickbuilds.tasks.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DueLabelsTest {
    private val today = LocalDate.of(2026, 11, 27) // a Friday
    private fun label(days: Long) = DueLabels.label(today.plusDays(days), today, Locale.UK)

    @Test fun relativeWords() {
        assertEquals(DueLabel("Today", DueTone.Today), label(0))
        assertEquals(DueLabel("Tomorrow", DueTone.Upcoming), label(1))
        assertEquals(DueLabel("Yesterday", DueTone.Overdue), label(-1))
    }

    @Test fun weekdayWithinAWeek() {
        assertEquals("Sun", label(2).text)
        assertEquals("Thu", label(6).text)
    }

    @Test fun datesFurtherOut() {
        assertEquals("4 Dec", label(7).text)
        assertEquals(DueTone.Overdue, label(-3).tone)
        assertEquals("24 Nov", label(-3).text)
        assertEquals("1 Jan 2027", label(35).text)
    }

    @Test fun longForm() {
        assertEquals("Fri 28 Nov", DueLabels.long(LocalDate.of(2025, 11, 28), LocalDate.of(2025, 1, 1), Locale.UK))
    }
}

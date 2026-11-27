package com.warwickbuilds.tasks.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class DueTone { Upcoming, Today, Overdue }

data class DueLabel(val text: String, val tone: DueTone)

/** The quiet due-date label: "Today", "Tomorrow", "Yesterday", "Sat", "28 Nov" or "28 Nov 2027". */
object DueLabels {
    fun label(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): DueLabel {
        val days = ChronoUnit.DAYS.between(today, date)
        val tone = when {
            days < 0 -> DueTone.Overdue
            days == 0L -> DueTone.Today
            else -> DueTone.Upcoming
        }
        val text = when (days) {
            0L -> "Today"
            1L -> "Tomorrow"
            -1L -> "Yesterday"
            in 2L..6L -> DateTimeFormatter.ofPattern("EEE", locale).format(date)
            else -> if (date.year == today.year) {
                DateTimeFormatter.ofPattern("d MMM", locale).format(date)
            } else {
                DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date)
            }
        }
        return DueLabel(text, tone)
    }

    /** The longer form used on the editor chip: "Fri 28 Nov". */
    fun long(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String =
        if (date.year == today.year) {
            DateTimeFormatter.ofPattern("EEE d MMM", locale).format(date)
        } else {
            DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale).format(date)
        }
}

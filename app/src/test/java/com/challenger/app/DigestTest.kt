package com.challenger.app

import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.domain.Digest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DigestTest {

    private val day = LocalDate.of(2026, 10, 9)
    private val eight = LocalTime.of(8, 0)

    @Test
    fun `до времени сводки она сегодня`() {
        assertEquals(
            day.atTime(eight),
            Digest.nextAt(day.atTime(6, 30), eight)
        )
    }

    @Test
    fun `после времени сводки она завтра`() {
        assertEquals(
            day.plusDays(1).atTime(eight),
            Digest.nextAt(day.atTime(9, 15), eight)
        )
    }

    @Test
    fun `ровно в момент сводки следующая — завтра, а не та же самая`() {
        // Иначе приёмник, переставляя будильник, поставил бы его на «сейчас»
        // и сводка пришла бы дважды.
        assertEquals(
            day.plusDays(1).atTime(eight),
            Digest.nextAt(LocalDateTime.of(day, eight), eight)
        )
    }

    @Test
    fun `обязательное идёт первым`() {
        fun c(id: Long, p: Priority, order: Int) =
            Challenge(id = id, title = "#$id", priority = p, sortOrder = order)

        val ordered = Digest.ordered(
            listOf(
                c(1, Priority.OPTIONAL, 0),
                c(2, Priority.MUST, 5),
                c(3, Priority.NORMAL, 1),
                c(4, Priority.MUST, 2)
            )
        )
        assertEquals(listOf(4L, 2L, 3L, 1L), ordered.map { it.id })
    }

    @Test
    fun `минуты от полуночи переводятся во время`() {
        assertEquals(LocalTime.of(8, 0), Digest.timeOf(480))
        assertEquals(LocalTime.of(23, 59), Digest.timeOf(1439))
    }
}

package com.challenger.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Пауза в несколько дней — отпуск, поездка, болезнь. Дни внутри окна выпадают
 * из расписания целиком: ни напоминаний, ни пропусков, серия их просто
 * перешагивает.
 *
 * Начать можно с сегодняшнего дня — болезнь не предупреждает заранее. Но не
 * задним числом: вчерашний пропуск паузой не закрыть. Ставить её каждый день
 * мешает годовой лимит, а не дата начала.
 */
@Entity(tableName = "freezes")
data class Freeze(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
) {
    val days: Int
        get() = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt().coerceAtLeast(0)

    fun covers(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)

    fun overlaps(other: Freeze): Boolean =
        !endDate.isBefore(other.startDate) && !other.endDate.isBefore(startDate)

    fun hasStarted(today: LocalDate = LocalDate.now()): Boolean = !today.isBefore(startDate)

    fun isOver(today: LocalDate = LocalDate.now()): Boolean = today.isAfter(endDate)

    /**
     * Пауза, оборванная сегодняшним днём: выздоровел раньше — с завтра снова
     * в строю, а неиспользованные дни возвращаются в лимит.
     *
     * Сегодня остаётся на паузе. Иначе её можно было бы держать весь день
     * и снимать вечером только тогда, когда всё сделано, — то есть получать
     * засчитанные дни и бесплатные пропуски одновременно.
     */
    fun endedEarly(today: LocalDate = LocalDate.now()): Freeze? = when {
        !covers(today) -> null
        endDate == today -> null
        else -> copy(endDate = today)
    }

    companion object {
        /** Сколько дней паузы можно потратить за календарный год. */
        const val DAYS_PER_YEAR = 14

        /** Самое раннее начало — сегодня: задним числом паузу не открыть. */
        fun earliestStart(today: LocalDate = LocalDate.now()): LocalDate = today

        /**
         * Сколько дней паузы осталось в году. Окно может пересекать Новый год,
         * поэтому считаем по дням, а не по дате начала.
         */
        fun daysLeftIn(year: Int, freezes: List<Freeze>): Int {
            val spent = freezes.sumOf { freeze ->
                (0 until freeze.days).count {
                    freeze.startDate.plusDays(it.toLong()).year == year
                }
            }
            return (DAYS_PER_YEAR - spent).coerceAtLeast(0)
        }
    }
}

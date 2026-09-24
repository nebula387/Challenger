package com.challenger.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "🔥",
    val category: Category = Category.SPORT,
    val priority: Priority = Priority.NORMAL,

    /** Цель на один день, 0 — просто отметка «сделал». */
    val targetValue: Int = 0,
    /** Единица измерения цели: раз, мин, км, стр. Заполняется из ресурсов при создании. */
    val unit: String = "",

    val startDate: LocalDate = LocalDate.now(),
    /** Срок челленджа в днях, 0 — бессрочно. */
    val durationDays: Int = 30,

    val scheduleType: ScheduleType = ScheduleType.EVERY_DAY,
    /** Используется только при [ScheduleType.WEEKDAYS]. */
    val weekdaysMask: Int = Weekdays.ALL,

    val remindersEnabled: Boolean = true,
    /** Времена напоминаний в течение дня. */
    val reminderTimes: List<LocalTime> = listOf(LocalTime.of(9, 0)),

    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Последний день челленджа включительно; null — бессрочный. */
    val endDate: LocalDate?
        get() = if (durationDays > 0) startDate.plusDays((durationDays - 1).toLong()) else null

    fun isFinished(today: LocalDate = LocalDate.now()): Boolean =
        endDate?.let { today.isAfter(it) } ?: false
}

/** Отметка выполнения за конкретный день. */
@Entity(tableName = "completions", primaryKeys = ["challengeId", "date"])
data class Completion(
    val challengeId: Long,
    val date: LocalDate,
    val doneAt: Long = System.currentTimeMillis(),
    /** Сколько сделал по факту (для целевых челленджей). */
    val value: Int = 0
)

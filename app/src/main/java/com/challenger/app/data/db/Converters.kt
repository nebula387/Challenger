package com.challenger.app.data.db

import androidx.room.TypeConverter
import com.challenger.app.data.model.Category
import com.challenger.app.data.model.Priority
import com.challenger.app.data.model.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun dateToLong(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    /** Список времён хранится как "540,1170" — минуты от полуночи. */
    @TypeConverter
    fun timesToString(value: List<LocalTime>?): String =
        value.orEmpty().joinToString(",") { (it.hour * 60 + it.minute).toString() }

    @TypeConverter
    fun stringToTimes(value: String?): List<LocalTime> =
        value.orEmpty().split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { LocalTime.of(it / 60, it % 60) }

    @TypeConverter fun categoryToString(value: Category): String = value.name
    @TypeConverter fun stringToCategory(value: String): Category =
        runCatching { Category.valueOf(value) }.getOrDefault(Category.OTHER)

    @TypeConverter fun priorityToString(value: Priority): String = value.name
    @TypeConverter fun stringToPriority(value: String): Priority =
        runCatching { Priority.valueOf(value) }.getOrDefault(Priority.NORMAL)

    @TypeConverter fun scheduleToString(value: ScheduleType): String = value.name
    @TypeConverter fun stringToSchedule(value: String): ScheduleType =
        runCatching { ScheduleType.valueOf(value) }.getOrDefault(ScheduleType.EVERY_DAY)
}

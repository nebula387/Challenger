package com.challenger.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.challenger.app.widget.TodayWidget
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Страховка на случай, если будильник потеряли: раз в сутки после полуночи
 * пересобираем расписание на новый день и обновляем виджет.
 */
class DailyRescheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Notifications.ensureChannels(applicationContext)
        ReminderScheduler.rescheduleAll(applicationContext)
        TodayWidget.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "daily_reschedule"

        fun enqueue(context: Context) {
            val now = LocalDateTime.now()
            val nextRun = now.toLocalDate().plusDays(1).atTime(LocalTime.of(0, 5))
            val delay = Duration.between(now, nextRun).coerceAtLeast(Duration.ofMinutes(1))

            val request = PeriodicWorkRequestBuilder<DailyRescheduleWorker>(Duration.ofDays(1))
                .setInitialDelay(delay)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

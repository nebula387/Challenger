package com.challenger.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.challenger.app.data.db.ChallengerDatabase
import com.challenger.app.domain.Schedule
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Держит в системе ровно один будильник на челлендж: ближайшее напоминание.
 * После каждой отметки и каждого срабатывания расписание пересобирается заново,
 * поэтому выполненный день больше не тревожит.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        val db = ChallengerDatabase.get(app)
        val challenges = db.challengeDao().getActive()
        val completionDao = db.completionDao()
        val now = LocalDateTime.now()

        for (challenge in challenges) {
            cancel(app, challenge.id)
            val doneDates = completionDao.getByChallenge(challenge.id).map { it.date }.toSet()
            val at = Schedule.nextReminderAt(challenge, now, doneDates) ?: continue
            schedule(app, challenge.id, at)
        }
    }

    suspend fun rescheduleOne(context: Context, challengeId: Long) {
        val app = context.applicationContext
        val db = ChallengerDatabase.get(app)
        val challenge = db.challengeDao().getById(challengeId) ?: return
        cancel(app, challengeId)
        if (challenge.archived) return

        val doneDates = db.completionDao().getByChallenge(challengeId).map { it.date }.toSet()
        val at = Schedule.nextReminderAt(challenge, LocalDateTime.now(), doneDates) ?: return
        schedule(app, challengeId, at)
    }

    fun schedule(context: Context, challengeId: Long, at: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = alarmIntent(context, challengeId)

        try {
            if (canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            } else {
                // Без разрешения на точные будильники система сама выберет окно.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Не удалось поставить точный будильник для " + challengeId, e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel(context: Context, challengeId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmIntent(context, challengeId))
    }

    /** Отложить напоминание на заданное число минут от текущего момента. */
    fun snooze(context: Context, challengeId: Long, minutes: Long = 60) {
        schedule(context, challengeId, LocalDateTime.now().plusMinutes(minutes))
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    private fun alarmIntent(context: Context, challengeId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(challengeId),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SHOW
                putExtra(ReminderReceiver.EXTRA_CHALLENGE_ID, challengeId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun requestCode(challengeId: Long): Int = 100_000 + challengeId.toInt()
}

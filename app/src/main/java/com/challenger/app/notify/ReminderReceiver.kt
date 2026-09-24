package com.challenger.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.challenger.app.data.repo.ChallengeRepository
import com.challenger.app.domain.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Всё, что приходит от будильника и от кнопок в шторке.
 * Отметка «Выполнил» работает без открытия приложения.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val challengeId = intent.getLongExtra(EXTRA_CHALLENGE_ID, -1L)
        if (challengeId <= 0) return

        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ChallengeRepository(app)
                when (intent.action) {
                    ACTION_SHOW -> handleShow(app, repo, challengeId)
                    ACTION_MARK_DONE -> handleMarkDone(app, repo, challengeId)
                    ACTION_SNOOZE -> handleSnooze(app, challengeId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleShow(
        context: Context,
        repo: ChallengeRepository,
        challengeId: Long
    ) {
        val challenge = repo.getById(challengeId) ?: return
        val today = LocalDate.now()

        // Пока будильник ждал, день мог быть отмечен или челлендж отредактирован.
        val alreadyDone = repo.doneDates(challengeId).contains(today)
        if (!alreadyDone && Schedule.isActiveOn(challenge, today)) {
            Notifications.showReminder(context, challenge)
        }
        ReminderScheduler.rescheduleOne(context, challengeId)
    }

    private suspend fun handleMarkDone(
        context: Context,
        repo: ChallengeRepository,
        challengeId: Long
    ) {
        repo.setDone(challengeId, LocalDate.now(), true)
        Notifications.cancel(context, challengeId)
        // setDone уже пересобрал расписание и обновил виджет.
    }

    private fun handleSnooze(context: Context, challengeId: Long) {
        Notifications.cancel(context, challengeId)
        ReminderScheduler.snooze(context, challengeId, SNOOZE_MINUTES)
    }

    companion object {
        const val ACTION_SHOW = "com.challenger.app.action.SHOW_REMINDER"
        const val ACTION_MARK_DONE = "com.challenger.app.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.challenger.app.action.SNOOZE"

        const val EXTRA_CHALLENGE_ID = "challenge_id"

        private const val SNOOZE_MINUTES = 60L

        fun requestCodeDone(challengeId: Long): Int = 200_000 + challengeId.toInt()
        fun requestCodeSnooze(challengeId: Long): Int = 300_000 + challengeId.toInt()
    }
}

package com.challenger.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.challenger.app.data.repo.ChallengeRepository
import com.challenger.app.ui.companion.CompanionFaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Срабатывание утренней сводки. Решает в моменте, показывать ли её: в день
 * паузы и в день, где ничего не осталось, уведомление было бы шумом.
 */
class DigestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ChallengeRepository(app)
                // Снимок дня уже без пауз и архива, остаётся отбросить сделанное.
                val left = repo.todaySnapshot().filterNot { it.isDone }.map { it.challenge }
                if (left.isNotEmpty()) {
                    val face = CompanionFaces.load(app, repo.todayMood(), FACE_PX)
                    Notifications.showDigest(app, left, face)
                }
            } finally {
                // Следующую сводку ставим в любом случае — иначе одна пропущенная
                // остановила бы их навсегда.
                ReminderScheduler.scheduleDigest(app)
                pending.finish()
            }
        }
    }

    private companion object {
        const val FACE_PX = 192
    }
}

package com.challenger.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.challenger.app.MainActivity
import com.challenger.app.R
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.domain.Digest

object Notifications {

    const val CHANNEL_MUST = "reminders_must"
    const val CHANNEL_NORMAL = "reminders_normal"
    const val CHANNEL_DIGEST = "daily_digest"

    /** Не пересекается с id челленджей: те растут с единицы. */
    private const val DIGEST_ID = Int.MAX_VALUE - 1

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val must = NotificationChannel(
            CHANNEL_MUST,
            context.getString(R.string.channel_must_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_must_desc)
            enableVibration(true)
        }

        val normal = NotificationChannel(
            CHANNEL_NORMAL,
            context.getString(R.string.channel_normal_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_normal_desc)
        }

        // Свой канал, чтобы сводку можно было приглушить в системе отдельно
        // от напоминаний — это разные по срочности вещи.
        val digest = NotificationChannel(
            CHANNEL_DIGEST,
            context.getString(R.string.channel_digest_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_digest_desc)
        }

        manager.createNotificationChannel(must)
        manager.createNotificationChannel(normal)
        manager.createNotificationChannel(digest)
    }

    /**
     * Картина дня одним уведомлением: каждое дело своей строкой, обязательные
     * сверху. Сворачивается в заголовок, разворачивается в список.
     */
    fun showDigest(context: Context, challenges: List<Challenge>, face: Bitmap?) {
        ensureChannels(context)
        val ordered = Digest.ordered(challenges)
        val mustCount = ordered.count { it.priority == Priority.MUST }

        val openApp = PendingIntent.getActivity(
            context,
            DIGEST_ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.resources.getQuantityString(
            R.plurals.digest_title, ordered.size, ordered.size
        )
        val summary = if (mustCount > 0) {
            context.getString(R.string.digest_must_count, mustCount)
        } else {
            null
        }

        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        ordered.forEach { style.addLine(digestLine(it)) }
        summary?.let(style::setSummaryText)

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            // В свёрнутом виде видно, с чего начать.
            .setContentText(ordered.take(3).joinToString(", ") { it.title })
            .setStyle(style)
            .setLargeIcon(face)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(DIGEST_ID, notification) }
    }

    private fun digestLine(challenge: Challenge): String {
        val goal = if (challenge.targetValue > 0) {
            " — " + challenge.targetValue + " " + challenge.unit
        } else {
            ""
        }
        return challenge.emoji + " " + challenge.title + goal
    }

    /**
     * Уведомление с кнопкой «Выполнил»: отметить можно прямо из шторки,
     * не открывая приложение.
     */
    fun showReminder(context: Context, challenge: Challenge, face: Bitmap? = null) {
        ensureChannels(context)

        val channel = if (challenge.priority == Priority.MUST) CHANNEL_MUST else CHANNEL_NORMAL
        val id = challenge.id.toInt()

        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDone = PendingIntent.getBroadcast(
            context,
            ReminderReceiver.requestCodeDone(challenge.id),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_MARK_DONE
                putExtra(ReminderReceiver.EXTRA_CHALLENGE_ID, challenge.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze = PendingIntent.getBroadcast(
            context,
            ReminderReceiver.requestCodeSnooze(challenge.id),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SNOOZE
                putExtra(ReminderReceiver.EXTRA_CHALLENGE_ID, challenge.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val goal = if (challenge.targetValue > 0) {
            context.getString(R.string.notif_goal, challenge.targetValue, challenge.unit)
        } else {
            context.getString(R.string.notif_time_to)
        }
        // Пробел ставим здесь: в strings.xml хвостовой пробел всё равно срезается.
        val text = if (challenge.priority == Priority.MUST) {
            context.getString(R.string.notif_must_prefix) + " " + goal
        } else {
            goal
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(challenge.emoji + " " + challenge.title)
            .setContentText(text)
            .setPriority(
                if (challenge.priority == Priority.MUST) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Лицо в шторке: напоминание смотрит на тебя, а не просто лежит.
            .setLargeIcon(face)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_action_done), markDone)
            .addAction(0, context.getString(R.string.notif_action_snooze), snooze)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    fun cancel(context: Context, challengeId: Long) {
        NotificationManagerCompat.from(context).cancel(challengeId.toInt())
    }

    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}

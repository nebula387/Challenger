package com.challenger.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.challenger.app.MainActivity
import com.challenger.app.R
import com.challenger.app.data.model.Challenge
import com.challenger.app.data.model.Priority
import com.challenger.app.domain.Digest
import com.challenger.app.ui.companion.CompanionFaces

object Notifications {

    const val CHANNEL_MUST = "reminders_must"
    const val CHANNEL_NORMAL = "reminders_normal"
    const val CHANNEL_DIGEST = "daily_digest"

    private const val TAG = "Notifications"
    private const val SHORTCUT_ID = "companion"

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
    fun showDigest(
        context: Context,
        challenges: List<Challenge>,
        portrait: CompanionFaces.Portrait?
    ) {
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

        val message = buildString {
            append(title)
            ordered.forEach { append('\n').append(digestLine(it)) }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            // В свёрнутом виде видно, с чего начать.
            .setContentText(ordered.take(3).joinToString(", ") { it.title })
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .asMessageFrom(context, portrait, message)
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
    fun showReminder(
        context: Context,
        challenge: Challenge,
        portrait: CompanionFaces.Portrait? = null
    ) {
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
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_action_done), markDone)
            .addAction(0, context.getString(R.string.notif_action_snooze), snooze)
            .asMessageFrom(context, portrait, challenge.emoji + " " + challenge.title + "\n" + text)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    /**
     * Делает уведомление сообщением от неё. Только у переписок Android рисует
     * слева крупный аватар вместо значка приложения — значок остаётся маленьким
     * бейджем на лице. Для этого нужны стиль переписки и долгоживущий ярлык
     * с её портретом. Без портрета уведомление остаётся обычным.
     */
    private fun NotificationCompat.Builder.asMessageFrom(
        context: Context,
        portrait: CompanionFaces.Portrait?,
        text: CharSequence
    ): NotificationCompat.Builder {
        if (portrait == null) return this

        val icon = IconCompat.createWithBitmap(portrait.face)
        val her = Person.Builder()
            .setKey(SHORTCUT_ID)
            .setName(portrait.name)
            .setIcon(icon)
            .setImportant(true)
            .build()

        // Если ярлык не встал, переписка без него показалась бы обычным
        // уведомлением без лица вовсе — тогда хотя бы кладём лицо справа.
        if (!publishShortcut(context, her, icon)) return setLargeIcon(portrait.face)

        val me = Person.Builder().setName(context.getString(R.string.notif_me)).build()
        return setShortcutId(SHORTCUT_ID)
            .setStyle(
                NotificationCompat.MessagingStyle(me)
                    .addMessage(text, System.currentTimeMillis(), her)
            )
    }

    /** Ярлык обновляется перед каждым показом: лицо на нём — сегодняшнее. */
    private fun publishShortcut(context: Context, her: Person, icon: IconCompat): Boolean =
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel(her.name ?: context.getString(R.string.app_name))
                .setLongLived(true)
                .setPerson(her)
                .setIcon(icon)
                .setIntent(
                    Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
                )
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            true
        }.getOrElse {
            Log.w(TAG, "Ярлык спутницы не опубликован", it)
            false
        }

    fun cancel(context: Context, challengeId: Long) {
        NotificationManagerCompat.from(context).cancel(challengeId.toInt())
    }

    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}

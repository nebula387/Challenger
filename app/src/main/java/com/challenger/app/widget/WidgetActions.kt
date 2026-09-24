package com.challenger.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import com.challenger.app.data.repo.ChallengeRepository
import java.time.LocalDate

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}

/** Тап по строке виджета: ставит или снимает галочку за сегодня. */
class ToggleDoneAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val challengeId = parameters[challengeIdKey] ?: return
        val repo = ChallengeRepository(context)
        repo.toggleDone(challengeId, LocalDate.now())
        TodayWidget().update(context, glanceId)
    }

    companion object {
        val challengeIdKey = ActionParameters.Key<Long>("challenge_id")
    }
}

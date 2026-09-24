package com.challenger.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.challenger.app.MainActivity
import com.challenger.app.R
import com.challenger.app.data.model.Priority
import com.challenger.app.data.repo.ChallengeRepository
import com.challenger.app.data.repo.TodayItem

/**
 * Виджет «Сегодня»: список дел на день прямо на рабочем столе.
 * Тап по строке ставит галочку, в приложение заходить не нужно.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = ChallengeRepository(context).todaySnapshot()
        provideContent {
            GlanceTheme {
                WidgetBody(items)
            }
        }
    }

    companion object {
        /** Перерисовать все размещённые копии виджета. */
        suspend fun refresh(context: Context) {
            runCatching { TodayWidget().updateAll(context.applicationContext) }
        }
    }
}

private val Green = ColorProvider(Color(0xFF2E7D32))
private val Red = ColorProvider(Color(0xFFC62828))
private val Muted = ColorProvider(Color(0xFF9E9E9E))

@androidx.compose.runtime.Composable
private fun WidgetBody(items: List<TodayItem>) {
    val context = LocalContext.current
    val done = items.count { it.isDone }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_today),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = done.toString() + " / " + items.size,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (items.isNotEmpty() && done == items.size) Green else Muted
                )
            )
        }

        if (items.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_empty),
                style = TextStyle(fontSize = 13.sp, color = Muted),
                modifier = GlanceModifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(top = 6.dp)) {
                items(items, itemId = { it.challenge.id }) { item ->
                    WidgetRow(item)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetRow(item: TodayItem) {
    val challenge = item.challenge
    val mark = if (item.isDone) "✅" else "⬜"

    val titleColor = when {
        item.isDone -> Muted
        challenge.priority == Priority.MUST -> Red
        else -> GlanceTheme.colors.onSurface
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(
                actionRunCallback<ToggleDoneAction>(
                    androidx.glance.action.actionParametersOf(
                        ToggleDoneAction.challengeIdKey to challenge.id
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mark, style = TextStyle(fontSize = 15.sp))
        Text(text = " ", modifier = GlanceModifier.width(6.dp))
        Text(
            text = challenge.emoji + " " + challenge.title,
            style = TextStyle(
                fontSize = 14.sp,
                color = titleColor,
                fontWeight = if (challenge.priority == Priority.MUST && !item.isDone) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                }
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        if (challenge.targetValue > 0 && !item.isDone) {
            Text(
                text = challenge.targetValue.toString() + " " + challenge.unit,
                style = TextStyle(fontSize = 12.sp, color = Muted)
            )
        }
    }
}

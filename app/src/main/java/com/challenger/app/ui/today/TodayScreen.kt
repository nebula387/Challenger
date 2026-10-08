package com.challenger.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.challenger.app.R
import com.challenger.app.domain.CompanionMood
import com.challenger.app.domain.TodayStatus
import com.challenger.app.ui.theme.LocalIsDark
import com.challenger.app.ui.theme.color
import com.challenger.app.ui.companion.Companion
import com.challenger.app.ui.components.ChallengeCard
import com.challenger.app.ui.components.scheduleSummary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    onOpenChallenge: (Long) -> Unit,
    onEditChallenge: (Long) -> Unit,
    onAddChallenge: () -> Unit,
    viewModel: TodayViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val companion by viewModel.companionSettings.collectAsStateWithLifecycle()

    // Отметил дело — спутница на пару секунд показывает «класс» и возвращается
    // к обычному настроению. Это и есть вся награда за галочку.
    var praising by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.praise.collect {
            praising = true
            delay(PRAISE_MILLIS)
            praising = false
        }
    }
    val mood = if (praising) CompanionMood.PRAISE else state.mood

    Box(modifier = Modifier.fillMaxSize()) {
        // Спутница живёт на фоне: карточки со своим фоном ложатся поверх неё.
        Companion(
            mood = mood,
            settings = companion,
            modifier = if (companion.fullScreen) {
                Modifier.matchParentSize()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .padding(bottom = 12.dp)
            }
        )

        // На весь экран заголовок оказывается поверх фотографии, поэтому
        // притеняем верх — иначе дату и прогресс не прочитать.
        if (companion.enabled && companion.fullScreen) {
            val background = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                background.copy(alpha = 0.92f),
                                background.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }

        // Со спутницей во весь экран карточки прижаты к низу: тогда они накрывают
        // её с ног, а лицо освобождается последним. Без неё — обычный порядок
        // сверху вниз, так список читается привычнее.
        val bottomAnchored = companion.enabled && companion.fullScreen

        // Сделанное из списка уходит: место в кадре дороже, чем перечёркнутая
        // строка. Остаётся счётчик, по нему всегда можно развернуть и посмотреть.
        val mustLeft = state.must.filterNot { it.isDone }
        val restLeft = state.rest.filterNot { it.isDone }
        val doneRows = state.all.filter { it.isDone }
        var showDone by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Дата и прогресс закреплены сверху и со списком не уезжают.
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                DayHeader(state)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Как только список грозит дорасти до лица, карточки ужимаются вдвое.
                val visibleRows = mustLeft.size + restLeft.size +
                    if (showDone) doneRows.size else 0
                val sections = listOf(mustLeft, restLeft).count { it.isNotEmpty() }
                val needed = FULL_ROW * visibleRows + SECTION_ROW * sections +
                    if (doneRows.isEmpty()) 0.dp else SECTION_ROW
                val compact = bottomAnchored && needed > maxHeight * FACE_HEADROOM

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = if (bottomAnchored) {
                        Arrangement.spacedBy(10.dp, Alignment.Bottom)
                    } else {
                        Arrangement.spacedBy(10.dp)
                    }
                ) {
                    if (!state.loading && state.total == 0) {
                        item {
                            val paused = state.pausedUntil
                            if (paused != null) PausedToday() else EmptyToday(onAddChallenge)
                        }
                    }

                    if (doneRows.isNotEmpty()) {
                        item(key = "done_summary") {
                            DoneSummary(
                                count = doneRows.size,
                                expanded = showDone,
                                expandsUpward = bottomAnchored,
                                onClick = { showDone = !showDone }
                            )
                        }
                        if (showDone) {
                            items(doneRows, key = { "done_" + it.challenge.id }) { row ->
                                TodayCard(row, viewModel, onOpenChallenge, compact = true)
                            }
                        }
                    }

                    if (mustLeft.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.today_section_must)) }
                        items(mustLeft, key = { it.challenge.id }) { row ->
                            TodayCard(row, viewModel, onOpenChallenge, compact)
                        }
                    }

                    if (restLeft.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.today_section_rest)) }
                        items(restLeft, key = { it.challenge.id }) { row ->
                            TodayCard(row, viewModel, onOpenChallenge, compact)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Свёрнутая строка выполненного: галочка, счётчик и разворот по нажатию.
 *
 * Стрелка показывает туда, куда поедет список. При [expandsUpward] он прижат
 * к низу экрана и растёт вверх, поэтому обычное направление перевёрнуто.
 */
@Composable
private fun DoneSummary(
    count: Int,
    expanded: Boolean,
    expandsUpward: Boolean,
    onClick: () -> Unit
) {
    val dark = LocalIsDark.current
    val green = TodayStatus.DONE.color(dark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Та же подложка, что у карточек: без неё счётчик теряется на картинке.
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.today_done_count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        val pointsUp = if (expandsUpward) !expanded else expanded
        Icon(
            imageVector = if (pointsUp) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayCard(
    row: TodayRow,
    viewModel: TodayViewModel,
    onOpenChallenge: (Long) -> Unit,
    compact: Boolean = false
) {
    val challenge = row.challenge
    val context = LocalContext.current
    val goal = if (challenge.targetValue > 0) {
        challenge.targetValue.toString() + " " + challenge.unit + "  ·  "
    } else {
        ""
    }
    val progress = if (challenge.durationDays > 0) {
        "  ·  " + stringResource(
            R.string.day_x_of_y, row.dayNumber, challenge.durationDays
        )
    } else {
        ""
    }

    ChallengeCard(
        challenge = challenge,
        status = row.status,
        subtitle = goal + scheduleSummary(context, challenge) + progress,
        streak = row.streak,
        onToggle = { viewModel.toggle(challenge.id) },
        onClick = { onOpenChallenge(challenge.id) },
        compact = compact
    )
}

@Composable
private fun DayHeader(state: TodayUiState) {
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
    val title = state.date.format(formatter).replaceFirstChar { it.uppercase(locale) }

    val paused = state.pausedUntil
    val subtitle = when {
        paused != null -> stringResource(R.string.today_paused_until, paused.shortDate())
        state.total == 0 -> stringResource(R.string.today_nothing_planned)
        state.allDone -> stringResource(R.string.today_all_done)
        state.mustLeft > 0 -> stringResource(R.string.today_must_left, state.mustLeft)
        else -> stringResource(R.string.today_must_clear)
    }

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (state.total > 0) {
                Text(
                    text = state.doneCount.toString() + " / " + state.total,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (state.total > 0) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.doneCount.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyToday(onAddChallenge: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.today_free_day),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.today_free_day_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAddChallenge) { Text(stringResource(R.string.today_pick_challenge)) }
    }
}

/** Сколько держится реакция «класс» после отметки. */
private const val PRAISE_MILLIS = 2500L

/** Оценка высоты строки списка: обычная карточка и заголовок раздела. */
private val FULL_ROW = 86.dp
private val SECTION_ROW = 34.dp

/** Какую долю свободной высоты можно занять, не доходя до лица спутницы. */
private const val FACE_HEADROOM = 0.62f

/** Пауза: напоминаний нет, и это нормально, а не повод добавлять челленджи. */
@Composable
private fun PausedToday() {
    Text(
        text = stringResource(R.string.today_paused_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Без подложки текст теряется на картинке за ним.
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

private fun LocalDate.shortDate(): String =
    format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))

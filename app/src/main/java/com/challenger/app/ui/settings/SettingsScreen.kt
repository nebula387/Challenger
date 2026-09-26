package com.challenger.app.ui.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.challenger.app.R
import com.challenger.app.notify.Notifications
import com.challenger.app.notify.ReminderScheduler
import com.challenger.app.widget.TodayWidgetReceiver

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val companion by viewModel.companion.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableStateOf(0) }

    // Разрешения выдают в системных настройках, поэтому статус перечитываем на возврате.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationsOn = remember(refreshKey) { Notifications.areEnabled(context) }
    val exactAlarmsOn = remember(refreshKey) { ReminderScheduler.canScheduleExact(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        SettingCard(
            title = stringResource(R.string.settings_notifications),
            status = stringResource(
                if (notificationsOn) R.string.settings_notifications_on
                else R.string.settings_notifications_off
            ),
            ok = notificationsOn,
            action = stringResource(R.string.settings_open),
            onAction = { context.openAppNotificationSettings() }
        )

        SettingCard(
            title = stringResource(R.string.settings_exact),
            status = stringResource(
                if (exactAlarmsOn) R.string.settings_exact_on else R.string.settings_exact_off
            ),
            ok = exactAlarmsOn,
            action = stringResource(R.string.settings_allow),
            onAction = { context.openExactAlarmSettings() }
        )

        SettingCard(
            title = stringResource(R.string.settings_battery),
            status = stringResource(R.string.settings_battery_hint),
            ok = true,
            action = stringResource(R.string.settings_battery_open),
            onAction = { context.openBatterySettings() }
        )

        CompanionSettingsCard(
            settings = companion,
            packs = packs,
            onEnabled = viewModel::setEnabled,
            onPack = viewModel::setPack,
            onOption = viewModel::setOption,
            onOpacity = viewModel::setOpacity,
            onFullScreen = viewModel::setFullScreen
        )

        SettingCard(
            title = stringResource(R.string.settings_widget),
            status = stringResource(R.string.settings_widget_hint),
            ok = true,
            action = stringResource(R.string.settings_widget_add),
            onAction = { context.requestPinWidget() }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    status: String,
    ok: Boolean,
    action: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(if (ok) "✅" else "⚠️")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onAction, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(action)
            }
        }
    }
}

private fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.parse("package:" + packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun Context.openBatterySettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:" + packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun Context.requestPinWidget() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(AppWidgetManager::class.java) ?: return
    val provider = ComponentName(this, TodayWidgetReceiver::class.java)
    if (!manager.isRequestPinAppWidgetSupported) return

    val callback = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, TodayWidgetReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    runCatching { manager.requestPinAppWidget(provider, null, callback) }
}

package com.challenger.app

import android.app.Application
import android.content.Context
import com.challenger.app.data.prefs.CompanionPrefs
import com.challenger.app.data.repo.ChallengeRepository
import com.challenger.app.notify.DailyRescheduleWorker
import com.challenger.app.notify.Notifications
import com.challenger.app.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChallengerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)

        // Приложение могли убить, а телефон перезагрузить: на старте всегда
        // пересобираем будильники на актуальный день.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ReminderScheduler.rescheduleAll(this@ChallengerApp)
            DailyRescheduleWorker.enqueue(this@ChallengerApp)
        }
    }
}

/** Ручной DI: зависимостей мало, целый Hilt тут был бы лишним. */
class AppContainer(context: Context) {
    val repository: ChallengeRepository = ChallengeRepository(context.applicationContext)
    val companionPrefs: CompanionPrefs = CompanionPrefs(context.applicationContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ChallengerApp).container

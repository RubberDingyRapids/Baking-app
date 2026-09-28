package com.rubberdingyrapids.baking

import android.app.Application
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.timer.TimerManager
import com.rubberdingyrapids.baking.timer.TimerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Process-wide singletons. Small enough that a DI framework would be overkill. */
class BakingApp : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var store: RecipeStore
        private set
    lateinit var timers: TimerManager
        private set

    override fun onCreate() {
        super.onCreate()
        store = RecipeStore(filesDir)
        timers = TimerManager(this)
        TimerNotifications.ensureChannel(this)
        scope.launch { store.load() }
    }
}

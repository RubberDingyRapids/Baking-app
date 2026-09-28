package com.rubberdingyrapids.baking

import android.app.Application
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.data.SettingsStore
import com.rubberdingyrapids.baking.share.ImportCoordinator
import com.rubberdingyrapids.baking.share.RecipeCloud
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
    lateinit var settings: SettingsStore
        private set
    lateinit var cloud: RecipeCloud
        private set
    lateinit var imports: ImportCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        store = RecipeStore(filesDir)
        timers = TimerManager(this)
        settings = SettingsStore(this)
        cloud = RecipeCloud(settings)
        imports = ImportCoordinator(this, store, cloud, scope)
        TimerNotifications.ensureChannel(this)
        scope.launch { store.load() }
    }
}

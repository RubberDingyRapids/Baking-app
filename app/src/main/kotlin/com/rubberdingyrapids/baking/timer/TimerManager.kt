package com.rubberdingyrapids.baking.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class ActiveTimer(
    /** "<recipeId>/<stepId>" */
    val key: String,
    val label: String,
    val durationSeconds: Int,
    val endAtMillis: Long,
) {
    fun remainingSeconds(now: Long = System.currentTimeMillis()): Int =
        ((endAtMillis - now + 999) / 1000).toInt().coerceAtLeast(0)

    fun isFinished(now: Long = System.currentTimeMillis()): Boolean = now >= endAtMillis
}

/**
 * Owns every running bake/wait timer. Timers are persisted to preferences so a
 * countdown survives the process being killed, and an [AlarmManager] alarm
 * fires [TimerAlarmReceiver] at the end so the notification rings even when
 * the app is not in the foreground.
 */
class TimerManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val json = Json { ignoreUnknownKeys = true }

    private val _timers = MutableStateFlow(load())
    val timers: StateFlow<Map<String, ActiveTimer>> = _timers.asStateFlow()

    fun get(key: String): ActiveTimer? = _timers.value[key]

    fun start(key: String, label: String, durationSeconds: Int): ActiveTimer {
        cancel(key)
        val timer = ActiveTimer(key, label, durationSeconds, System.currentTimeMillis() + durationSeconds * 1000L)
        scheduleAlarm(timer)
        _timers.value = _timers.value + (key to timer)
        persist()
        return timer
    }

    /** Stops the alarm and forgets the timer. */
    fun cancel(key: String) {
        val existing = _timers.value[key] ?: return
        alarms.cancel(pendingIntent(existing))
        _timers.value = _timers.value - key
        persist()
    }

    /** Drops every timer belonging to a recipe, e.g. when cooking is finished. */
    fun cancelAll(recipeId: String) {
        _timers.value.keys.filter { it.startsWith("$recipeId/") }.forEach { cancel(it) }
    }

    private fun scheduleAlarm(timer: ActiveTimer) {
        val pi = pendingIntent(timer)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (canBeExact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAtMillis, pi)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endAtMillis, pi)
        }
    }

    private fun pendingIntent(timer: ActiveTimer): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_TIMER_DONE
            putExtra(TimerAlarmReceiver.EXTRA_KEY, timer.key)
            putExtra(TimerAlarmReceiver.EXTRA_LABEL, timer.label)
            data = android.net.Uri.parse("baking://timer/${timer.key}")
        }
        return PendingIntent.getBroadcast(
            context, timer.key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun persist() {
        val text = json.encodeToString(ListSerializer(ActiveTimer.serializer()), _timers.value.values.toList())
        prefs.edit().putString(KEY_TIMERS, text).apply()
    }

    private fun load(): Map<String, ActiveTimer> {
        val text = prefs.getString(KEY_TIMERS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString(ListSerializer(ActiveTimer.serializer()), text) }
            .getOrDefault(emptyList())
            .associateBy { it.key }
    }

    companion object {
        private const val PREFS = "timers"
        private const val KEY_TIMERS = "active"
        fun key(recipeId: String, stepId: String) = "$recipeId/$stepId"
    }
}

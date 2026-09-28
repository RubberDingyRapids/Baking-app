package com.rubberdingyrapids.baking.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_DONE) return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
        TimerNotifications.showTimerDone(context, key, label)
    }

    companion object {
        const val ACTION_TIMER_DONE = "com.rubberdingyrapids.baking.TIMER_DONE"
        const val EXTRA_KEY = "key"
        const val EXTRA_LABEL = "label"
    }
}

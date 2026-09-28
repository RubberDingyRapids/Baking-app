package com.rubberdingyrapids.baking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.rubberdingyrapids.baking.ui.BakingNavHost
import com.rubberdingyrapids.baking.ui.theme.BakingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so keep the system bar icons light on every device setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            BakingTheme {
                // Navigation state is saved with the activity, so the app comes back to the
                // same screen after being backgrounded and starts at the list after a full close.
                BakingNavHost()
            }
        }
    }
}

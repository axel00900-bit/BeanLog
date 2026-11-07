package com.example.beanlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.beanlog.ui.theme.BeanLogTheme

class MainActivity : ComponentActivity() {
    private var darkMode by mutableStateOf(false)
    private var pendingBeanId by mutableStateOf<Long?>(null)
    private var pendingBagId by mutableStateOf<Long?>(null)
    private var pendingBrewId by mutableStateOf<Long?>(null)
    private var navigationRequestId by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDb.create(applicationContext).dao()
        val preferences = getSharedPreferences("beanlog_settings", MODE_PRIVATE)
        darkMode = preferences.getBoolean("dark_theme", false)
        updateNotificationDestination(intent)
        setContent {
            BeanLogTheme(darkMode) {
                App(dao, pendingBeanId, pendingBagId, pendingBrewId, darkMode, navigationRequestId) { enabled ->
                    darkMode = enabled
                    preferences.edit().putBoolean("dark_theme", enabled).apply()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateNotificationDestination(intent)
    }

    private fun updateNotificationDestination(intent: android.content.Intent) {
        pendingBeanId = intent.getLongExtra("bean_id", -1L).takeIf { it > 0L }
        pendingBagId = intent.getLongExtra("bag_id", -1L).takeIf { it > 0L }
        pendingBrewId = intent.getLongExtra("shot_id", -1L).takeIf { it > 0L }
        if (pendingBeanId != null) navigationRequestId++
    }
}

package com.mystx.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.mystx.app.manager.CommandManager
import com.mystx.app.manager.StatsManager

class MystxViewModel(application: Application) : AndroidViewModel(application) {
    val prefs: SharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val keyManager = (application as MystxApp).keyManager
    val commandManager = CommandManager(application)
    val statsManager = StatsManager(application)
}

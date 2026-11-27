package com.warwickbuilds.tasks

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small on-device preferences. Nothing here leaves the phone. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _wallpaperColours = MutableStateFlow(prefs.getBoolean(KEY_WALLPAPER, false))
    val wallpaperColours: StateFlow<Boolean> = _wallpaperColours.asStateFlow()

    var lastGroup: String?
        get() = prefs.getString(KEY_GROUP, null)
        set(value) = prefs.edit { putString(KEY_GROUP, value) }

    fun setWallpaperColours(on: Boolean) {
        prefs.edit { putBoolean(KEY_WALLPAPER, on) }
        _wallpaperColours.value = on
    }

    private companion object {
        const val KEY_GROUP = "last_group"
        const val KEY_WALLPAPER = "wallpaper_colours"
    }
}

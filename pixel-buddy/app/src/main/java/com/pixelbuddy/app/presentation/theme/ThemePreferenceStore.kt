package com.pixelbuddy.app.presentation.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * 持久化用户选择的主题偏好。
 * 使用 DataStore（已存在于依赖中），轻量且可靠。
 */
@Singleton
class ThemePreferenceStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.themeDataStore

    /** 当前主题的 Flow */
    val themeFlow: Flow<AppTheme> = dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME] ?: AppTheme.PIXEL_BUDDY.name
        try {
            AppTheme.valueOf(name)
        } catch (_: IllegalArgumentException) {
            AppTheme.PIXEL_BUDDY
        }
    }

    /** 保存主题选择 */
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
    }
}

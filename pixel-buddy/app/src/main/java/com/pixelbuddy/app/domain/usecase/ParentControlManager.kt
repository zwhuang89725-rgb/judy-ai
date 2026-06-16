package com.pixelbuddy.app.domain.usecase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.parentDataStore: DataStore<Preferences> by preferencesDataStore(name = "parent_control")

/**
 * 家长控制管理。
 *
 * 记录每日使用时长，超时后阻止继续使用。
 * 提供内容过滤开关。
 */
@Singleton
class ParentControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.parentDataStore

    /** 家长控制是否启用 */
    val isEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: false }

    /** 每日最大使用分钟数 */
    val dailyLimitMinutes: Flow<Int> = dataStore.data.map { it[KEY_DAILY_LIMIT] ?: 30 }

    /** 内容过滤是否启用 */
    val contentFilterEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CONTENT_FILTER] ?: true }

    /** 今日已使用的分钟数 */
    val todayUsageMinutes: Flow<Int> = dataStore.data.map { prefs ->
        val date = prefs[KEY_USAGE_DATE] ?: ""
        if (date == todayDateString()) prefs[KEY_USAGE_TODAY] ?: 0 else 0
    }

    /** 是否已达今日限制 */
    val isLimited: Flow<Boolean> = dataStore.data.map { prefs ->
        val enabled = prefs[KEY_ENABLED] ?: false
        if (!enabled) return@map false
        val used = if (prefs[KEY_USAGE_DATE] == todayDateString()) prefs[KEY_USAGE_TODAY] ?: 0 else 0
        val limit = prefs[KEY_DAILY_LIMIT] ?: 30
        used >= limit
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setDailyLimit(minutes: Int) {
        dataStore.edit { it[KEY_DAILY_LIMIT] = minutes.coerceIn(5, 120) }
    }

    suspend fun setContentFilter(enabled: Boolean) {
        dataStore.edit { it[KEY_CONTENT_FILTER] = enabled }
    }

    /** 记录使用时间 */
    suspend fun recordUsage(minutes: Int = 1) {
        dataStore.edit { prefs ->
            val today = todayDateString()
            val used = if (prefs[KEY_USAGE_DATE] == today) prefs[KEY_USAGE_TODAY] ?: 0 else 0
            prefs[KEY_USAGE_DATE] = today
            prefs[KEY_USAGE_TODAY] = used + minutes
        }
    }

    /** 重置今日记录 */
    suspend fun resetToday() {
        dataStore.edit { prefs ->
            prefs[KEY_USAGE_DATE] = todayDateString()
            prefs[KEY_USAGE_TODAY] = 0
        }
    }

    private fun todayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    companion object {
        val KEY_ENABLED = booleanPreferencesKey("pc_enabled")
        val KEY_DAILY_LIMIT = intPreferencesKey("pc_daily_limit")
        val KEY_CONTENT_FILTER = booleanPreferencesKey("pc_content_filter")
        val KEY_USAGE_DATE = stringPreferencesKey("pc_usage_date")
        val KEY_USAGE_TODAY = intPreferencesKey("pc_usage_today")
    }
}

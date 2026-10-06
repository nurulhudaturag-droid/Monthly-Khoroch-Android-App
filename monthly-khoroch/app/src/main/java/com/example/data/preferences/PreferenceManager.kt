package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("monthly_khoroch_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(getLastBackupTime())
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _isOnboarded = MutableStateFlow(isOnboarded())
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _currencySymbol = MutableStateFlow(getCurrencySymbol())
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()
        _isOnboarded.value = value
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "system") ?: "system"

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun getLastBackupTime(): Long = prefs.getLong(KEY_LAST_BACKUP, 0L)

    fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
        _lastBackupTime.value = timestamp
    }

    fun getCurrencySymbol(): String = prefs.getString(KEY_CURRENCY_SYMBOL, "৳") ?: "৳"

    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
        _currencySymbol.value = symbol
    }

    companion object {
        private const val KEY_ONBOARDED = "is_onboarded"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_BACKUP = "last_backup_time"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    }
}

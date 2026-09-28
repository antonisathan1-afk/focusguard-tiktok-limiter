package com.focusguard.tiktoklimiter

import android.content.SharedPreferences

/**
 * Central place for all SharedPreferences keys and defaults used by the app.
 * MainActivity writes settings here; TikTokBlockerService reads them and
 * tracks live usage/lock state here too.
 */
object Prefs {
    const val NAME = "focusguard_prefs"

    // Settings (user configurable)
    const val KEY_USAGE_LIMIT_MS = "usage_limit_ms"
    const val KEY_LOCK_DURATION_MS = "lock_duration_ms"
    const val KEY_TARGET_PACKAGES = "target_packages"

    // Live state (managed by the service)
    const val KEY_USED_MS = "used_ms"
    const val KEY_LOCK_UNTIL = "lock_until"

    const val DEFAULT_USAGE_LIMIT_MS = 5 * 60 * 1000L
    const val DEFAULT_LOCK_DURATION_MS = 30 * 60 * 1000L

    // TikTok's package name differs slightly by region/build.
    // com.zhiliaoapp.musically -> global TikTok
    // com.ss.android.ugc.trill -> TikTok in some regions / older builds
    val DEFAULT_TARGET_PACKAGES = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    )

    fun getTargetPackages(prefs: SharedPreferences): Set<String> {
        val stored = prefs.getStringSet(KEY_TARGET_PACKAGES, null)
        return if (stored.isNullOrEmpty()) DEFAULT_TARGET_PACKAGES else stored
    }

    fun setTargetPackages(prefs: SharedPreferences, packages: Set<String>) {
        prefs.edit().putStringSet(KEY_TARGET_PACKAGES, packages).apply()
    }

    fun getUsageLimitMs(prefs: SharedPreferences): Long =
        prefs.getLong(KEY_USAGE_LIMIT_MS, DEFAULT_USAGE_LIMIT_MS)

    fun getLockDurationMs(prefs: SharedPreferences): Long =
        prefs.getLong(KEY_LOCK_DURATION_MS, DEFAULT_LOCK_DURATION_MS)
}

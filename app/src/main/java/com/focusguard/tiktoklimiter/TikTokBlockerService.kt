package com.focusguard.tiktoklimiter

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches which app is in the foreground. While a target app (TikTok, by
 * default) is open:
 *  - If we're still inside a "lock" period from a previous session, the app
 *    is kicked to the home screen immediately.
 *  - Otherwise, usage time accumulates once per second. Once the configured
 *    limit (default 5 min) is reached, a lock period starts (default 30 min)
 *    and the user is sent home. Usage resets to zero once the lock expires,
 *    so the next visit gets a fresh allowance.
 */
class TikTokBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var isTargetInForeground = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isTargetInForeground) {
                tick()
            }
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        handler.post(tickRunnable)
        Log.d(TAG, "FocusGuard accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        val targets = Prefs.getTargetPackages(prefs)
        if (pkg in targets) {
            isTargetInForeground = true
            enforceOnEntry()
        } else {
            isTargetInForeground = false
        }
    }

    /** Called the moment the target app comes to the foreground. */
    private fun enforceOnEntry() {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.getLong(Prefs.KEY_LOCK_UNTIL, 0L)
        if (now < lockUntil) {
            val remainingMin = ((lockUntil - now) / 60000L) + 1
            toast("التطبيق مقفول لسه — فاضل حوالي $remainingMin دقيقة")
            kickHome()
        }
    }

    /** Called once per second while the target app is in the foreground. */
    private fun tick() {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.getLong(Prefs.KEY_LOCK_UNTIL, 0L)

        if (now < lockUntil) {
            // Shouldn't normally happen (enforceOnEntry already kicks out),
            // but guard against edge cases (e.g. clock changes).
            kickHome()
            return
        }

        val limitMs = Prefs.getUsageLimitMs(prefs)
        val usedMs = prefs.getLong(Prefs.KEY_USED_MS, 0L) + TICK_INTERVAL_MS

        if (usedMs >= limitMs) {
            startLockPeriod()
        } else {
            prefs.edit().putLong(Prefs.KEY_USED_MS, usedMs).apply()
        }
    }

    private fun startLockPeriod() {
        val lockDurationMs = Prefs.getLockDurationMs(prefs)
        val lockUntil = System.currentTimeMillis() + lockDurationMs
        prefs.edit()
            .putLong(Prefs.KEY_LOCK_UNTIL, lockUntil)
            .putLong(Prefs.KEY_USED_MS, 0L)
            .apply()

        val lockMinutes = lockDurationMs / 60000L
        toast("خلّصت وقتك! التطبيق هيتقفل لمدة $lockMinutes دقيقة")
        kickHome()
    }

    private fun kickHome() {
        isTargetInForeground = false
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun toast(msg: String) {
        handler.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    override fun onInterrupt() {
        // Required override; nothing to clean up here.
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
    }

    companion object {
        private const val TAG = "TikTokBlockerService"
        private const val TICK_INTERVAL_MS = 1000L
    }
}

package com.focusguard.tiktoklimiter

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var limitMinutesInput: EditText
    private lateinit var lockMinutesInput: EditText
    private lateinit var packagesInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        limitMinutesInput = findViewById(R.id.limitMinutesInput)
        lockMinutesInput = findViewById(R.id.lockMinutesInput)
        packagesInput = findViewById(R.id.packagesInput)

        findViewById<Button>(R.id.enableAccessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveSettings()
        }

        loadSettingsIntoUi()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    private fun loadSettingsIntoUi() {
        val limitMin = Prefs.getUsageLimitMs(prefs) / 60000L
        val lockMin = Prefs.getLockDurationMs(prefs) / 60000L
        val packages = Prefs.getTargetPackages(prefs)

        limitMinutesInput.setText(limitMin.toString())
        lockMinutesInput.setText(lockMin.toString())
        packagesInput.setText(TextUtils.join(",", packages))
    }

    private fun saveSettings() {
        val limitMin = limitMinutesInput.text.toString().toLongOrNull()
        val lockMin = lockMinutesInput.text.toString().toLongOrNull()
        val packagesRaw = packagesInput.text.toString()

        if (limitMin == null || limitMin <= 0 || lockMin == null || lockMin <= 0) {
            Toast.makeText(this, "من فضلك دخّل أرقام صحيحة أكبر من صفر", Toast.LENGTH_SHORT).show()
            return
        }

        val packages = packagesRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (packages.isEmpty()) {
            Toast.makeText(this, "دخّل على الأقل اسم باكدج واحد", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putLong(Prefs.KEY_USAGE_LIMIT_MS, limitMin * 60000L)
            .putLong(Prefs.KEY_LOCK_DURATION_MS, lockMin * 60000L)
            .apply()
        Prefs.setTargetPackages(prefs, packages)

        Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText() {
        statusText.text = if (isAccessibilityServiceEnabled()) {
            "حالة صلاحية الوصول: مفعّلة ✅"
        } else {
            "حالة صلاحية الوصول: غير مفعّلة ❌ — دوس على الزرار تحت عشان تفعّلها"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "$packageName/${TikTokBlockerService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }
}

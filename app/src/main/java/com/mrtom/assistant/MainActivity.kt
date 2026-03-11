package com.mrtom.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mrtom.assistant.databinding.ActivityMainBinding

/**
 * MainActivity — Entry point for MR.TOM voice assistant.
 *
 * Requests all required permissions, then starts [AssistantService] as a
 * foreground service so it remains alive even when the screen is off.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // All permissions the assistant needs at runtime
    private val requiredPermissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val denied = results.filterValues { !it }.keys
            if (denied.isEmpty()) {
                log("✅ সব অনুমতি দেওয়া হয়েছে")
                startAssistantService()
            } else {
                log("⚠️ কিছু অনুমতি দেওয়া হয়নি: ${denied.joinToString()}")
                // Attempt to start with whatever was granted
                startAssistantService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartAssistant.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnStopAssistant.setOnClickListener {
            stopAssistantService()
        }

        binding.btnBatteryOptimization.setOnClickListener {
            openBatteryOptimizationSettings()
        }

        // Auto-start if already has permissions
        if (allPermissionsGranted()) {
            startAssistantService()
        }

        log("MR.TOM প্রস্তুত। শুরু করতে বোতাম চাপুন।")
    }

    private fun checkAndRequestPermissions() {
        if (allPermissionsGranted()) {
            startAssistantService()
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun allPermissionsGranted(): Boolean =
        requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.statusText.text = getString(R.string.status_running)
        binding.btnStartAssistant.isEnabled = false
        binding.btnStopAssistant.isEnabled = true
        log("🎙️ MR.TOM সক্রিয় — এখন কথা বলুন!")
    }

    private fun stopAssistantService() {
        val intent = Intent(this, AssistantService::class.java)
        stopService(intent)
        binding.statusText.text = getString(R.string.status_stopped)
        binding.btnStartAssistant.isEnabled = true
        binding.btnStopAssistant.isEnabled = false
        log("🛑 MR.TOM বন্ধ করা হয়েছে")
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    /** Append a log line to the on-screen log view. */
    fun log(message: String) {
        runOnUiThread {
            binding.logText.append("\n$message")
            binding.logScroll.post {
                binding.logScroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}

package com.mrtom.assistant

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import android.util.Log

/**
 * PhoneController — Wraps Android APIs for common phone-control operations.
 *
 * All public methods are safe to call from any thread; they post work to the
 * main thread when required.
 */
class PhoneController(private val context: Context) {

    companion object {
        private const val TAG = "PhoneController"
    }

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var torchOn = false

    /** Lazily-cached list of installed applications to avoid repeated enumeration. */
    private val installedApps by lazy {
        context.packageManager.getInstalledApplications(0)
    }

    // ── Phone calls ─────────────────────────────────────────────────────────────

    /**
     * Look up [name] in the device contacts and dial the first match.
     * Falls back to launching the dialler with the name as a search query.
     */
    fun callByName(name: String) {
        val number = lookupNumber(name)
        if (number != null) {
            Log.i(TAG, "Calling $name → $number")
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Log.w(TAG, "Contact not found: $name — opening dialler")
            val intent = Intent(Intent.ACTION_DIAL)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /** Open the SMS compose screen pre-addressed to [name]. */
    fun openSmsTo(name: String) {
        val number = lookupNumber(name)
        val uri = if (number != null) Uri.parse("smsto:$number") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ── App launcher ────────────────────────────────────────────────────────────

    /**
     * Launch an installed application by matching [appName] against
     * the label of every installed package (case-insensitive partial match).
     *
     * @return true if a matching app was found and launched.
     */
    fun launchApp(appName: String): Boolean {
        val pm = context.packageManager
        val packages = installedApps
        val query = appName.lowercase()

        // Well-known package-name shortcuts
        val wellKnown = mapOf(
            "whatsapp"  to "com.whatsapp",
            "youtube"   to "com.google.android.youtube",
            "facebook"  to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "camera"    to "com.android.camera2",
            "music"     to "com.google.android.music",
            "maps"      to "com.google.android.apps.maps",
            "gmail"     to "com.google.android.gm",
            "chrome"    to "com.android.chrome",
            "settings"  to "com.android.settings",
            "calculator" to "com.android.calculator2",
            "calendar"  to "com.android.calendar",
            "clock"     to "com.android.deskclock"
        )
        wellKnown[query]?.let { pkg ->
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        // Fuzzy match installed app labels
        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i(TAG, "Launched: ${appInfo.packageName}")
                    return true
                }
            }
        }
        Log.w(TAG, "App not found: $appName")
        return false
    }

    // ── System info ─────────────────────────────────────────────────────────────

    fun getBatteryLevel(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    // ── Flashlight ───────────────────────────────────────────────────────────────

    fun toggleFlashlight() {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            torchOn = !torchOn
            cm.setTorchMode(cameraId, torchOn)
            Log.i(TAG, "Torch: $torchOn")
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error: ${e.message}")
        }
    }

    // ── Volume ───────────────────────────────────────────────────────────────────

    fun adjustVolume(increase: Boolean) {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ── Ringer ───────────────────────────────────────────────────────────────────

    fun setRingerSilent() {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (e: Exception) {
            Log.e(TAG, "setRingerSilent: ${e.message}")
        }
    }

    fun setRingerNormal() {
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }

    // ── Contacts lookup ──────────────────────────────────────────────────────────

    /**
     * Returns the first phone number associated with a contact whose display name
     * contains [name] (case-insensitive). Returns null if no match is found or
     * if READ_CONTACTS permission is not granted.
     */
    fun lookupNumber(name: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            ) ?: return null

            cursor.use {
                while (it.moveToNext()) {
                    val displayName = it.getString(0) ?: continue
                    val number = it.getString(1) ?: continue
                    if (displayName.lowercase().contains(name.lowercase())) {
                        return number
                    }
                }
            }
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "READ_CONTACTS permission denied")
            null
        }
    }
}

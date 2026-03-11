package com.mrtom.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

/**
 * VoiceCommandProcessor — Translates a recognised voice string into an action.
 *
 * Supports commands in Bengali and English:
 *
 *  | Keyword(s)                    | Action                         |
 *  |-------------------------------|--------------------------------|
 *  | ফোন করো / call                | Make a phone call              |
 *  | মেসেজ / sms / text            | Send SMS                       |
 *  | খোলো / open / চালু করো        | Launch an app                  |
 *  | বন্ধ করো / stop               | No-op (handled by service)     |
 *  | সময় / time                   | Read current time              |
 *  | তারিখ / date                  | Read current date              |
 *  | ব্যাটারি / battery             | Read battery level             |
 *  | আলো / flashlight / torch      | Toggle flashlight              |
 *  | ভলিউম বাড়াও / volume up      | Raise media volume             |
 *  | ভলিউম কমাও / volume down     | Lower media volume             |
 *  | সাইলেন্ট / silent / mute      | Set ringer to silent           |
 *  | রিং / ring / রিংটোন           | Set ringer to normal           |
 *  | ওয়াইফাই / wifi               | Open Wi-Fi settings            |
 *  | ব্লুটুথ / bluetooth           | Open Bluetooth settings        |
 *  | ক্যামেরা / camera             | Open camera app                |
 *  | গান / music / play            | Open music app                 |
 *  | ব্রাউজার / browser / গুগল     | Open browser / Google          |
 *  | ম্যাপ / map / নেভিগেশন        | Open Maps                      |
 */
class VoiceCommandProcessor(
    private val context: Context,
    private val phoneController: PhoneController
) {

    companion object {
        private const val TAG = "VoiceCommandProcessor"
    }

    /**
     * Entry point. Returns a spoken reply for [AssistantService] to say aloud.
     */
    fun process(input: String): String {
        val text = input.trim().lowercase()
        Log.d(TAG, "Processing: $text")

        return when {
            // ── Phone call ──────────────────────────────────────────────────
            text.contains("ফোন করো") || text.contains("call") ||
            text.contains("কল করো") || text.contains("ডায়াল") -> {
                val name = extractNameAfterKeyword(text,
                    listOf("ফোন করো", "call", "কল করো", "ডায়াল"))
                if (name.isNotBlank()) {
                    phoneController.callByName(name)
                    "$name কে ফোন করছি"
                } else {
                    "কাকে ফোন করব?"
                }
            }

            // ── SMS ─────────────────────────────────────────────────────────
            text.contains("মেসেজ") || text.contains("sms") ||
            text.contains("text") || text.contains("এসএমএস") -> {
                val name = extractNameAfterKeyword(text,
                    listOf("মেসেজ করো", "sms করো", "text করো", "এসএমএস করো",
                           "মেসেজ দাও", "মেসেজ পাঠাও", "মেসেজ"))
                if (name.isNotBlank()) {
                    phoneController.openSmsTo(name)
                    "$name কে মেসেজ করার জন্য খুলছি"
                } else {
                    "কাকে মেসেজ করব?"
                }
            }

            // ── App launch ──────────────────────────────────────────────────
            text.contains("খোলো") || text.contains("open") ||
            text.contains("চালু করো") || text.contains("start") -> {
                val appName = extractNameAfterKeyword(text,
                    listOf("খোলো", "open", "চালু করো", "start"))
                if (appName.isNotBlank()) {
                    val launched = phoneController.launchApp(appName)
                    if (launched) "$appName খুলছি" else "$appName খুঁজে পাচ্ছি না"
                } else {
                    "কোন অ্যাপ খুলব?"
                }
            }

            // ── Time ────────────────────────────────────────────────────────
            text.contains("সময়") || text.contains("time") ||
            text.contains("কটা বাজে") || text.contains("ক'টা") -> {
                val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("bn"))
                    .format(java.util.Date())
                "এখন সময় $now"
            }

            // ── Date ────────────────────────────────────────────────────────
            text.contains("তারিখ") || text.contains("date") ||
            text.contains("আজকের") -> {
                val today = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("bn"))
                    .format(java.util.Date())
                "আজকের তারিখ $today"
            }

            // ── Battery ─────────────────────────────────────────────────────
            text.contains("ব্যাটারি") || text.contains("battery") ||
            text.contains("চার্জ") -> {
                val level = phoneController.getBatteryLevel()
                "ব্যাটারি $level শতাংশ"
            }

            // ── Flashlight ──────────────────────────────────────────────────
            text.contains("আলো") || text.contains("flashlight") ||
            text.contains("torch") || text.contains("টর্চ") -> {
                phoneController.toggleFlashlight()
                "টর্চ পরিবর্তন করা হয়েছে"
            }

            // ── Volume up ───────────────────────────────────────────────────
            text.contains("ভলিউম বাড়াও") || text.contains("volume up") ||
            text.contains("আওয়াজ বাড়াও") -> {
                phoneController.adjustVolume(increase = true)
                "ভলিউম বাড়ানো হয়েছে"
            }

            // ── Volume down ─────────────────────────────────────────────────
            text.contains("ভলিউম কমাও") || text.contains("volume down") ||
            text.contains("আওয়াজ কমাও") -> {
                phoneController.adjustVolume(increase = false)
                "ভলিউম কমানো হয়েছে"
            }

            // ── Silent mode ─────────────────────────────────────────────────
            text.contains("সাইলেন্ট") || text.contains("silent") ||
            text.contains("mute") || text.contains("নীরব") -> {
                phoneController.setRingerSilent()
                "সাইলেন্ট মোড চালু"
            }

            // ── Ringer normal ────────────────────────────────────────────────
            text.contains("রিং") || text.contains("ring") ||
            text.contains("রিংটোন") || text.contains("আওয়াজ দাও") -> {
                phoneController.setRingerNormal()
                "রিং মোড চালু"
            }

            // ── Wi-Fi settings ───────────────────────────────────────────────
            text.contains("ওয়াইফাই") || text.contains("wifi") ||
            text.contains("wi-fi") -> {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "ওয়াইফাই সেটিংস খুলছি"
            }

            // ── Bluetooth settings ───────────────────────────────────────────
            text.contains("ব্লুটুথ") || text.contains("bluetooth") -> {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "ব্লুটুথ সেটিংস খুলছি"
            }

            // ── Camera ───────────────────────────────────────────────────────
            text.contains("ক্যামেরা") || text.contains("camera") ||
            text.contains("ছবি তুলো") -> {
                phoneController.launchApp("camera")
                "ক্যামেরা খুলছি"
            }

            // ── Music ────────────────────────────────────────────────────────
            text.contains("গান") || text.contains("music") ||
            text.contains("play") || text.contains("সংগীত") -> {
                phoneController.launchApp("music")
                "মিউজিক প্লেয়ার খুলছি"
            }

            // ── Browser / Google ─────────────────────────────────────────────
            text.contains("ব্রাউজার") || text.contains("browser") ||
            text.contains("গুগল") || text.contains("google") ||
            text.contains("ইন্টারনেট") -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "ব্রাউজার খুলছি"
            }

            // ── Maps ─────────────────────────────────────────────────────────
            text.contains("ম্যাপ") || text.contains("map") ||
            text.contains("নেভিগেশন") || text.contains("navigation") -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "ম্যাপ খুলছি"
            }

            // ── YouTube ──────────────────────────────────────────────────────
            text.contains("ইউটিউব") || text.contains("youtube") -> {
                phoneController.launchApp("youtube")
                "ইউটিউব খুলছি"
            }

            // ── WhatsApp ─────────────────────────────────────────────────────
            text.contains("হোয়াটসঅ্যাপ") || text.contains("whatsapp") -> {
                phoneController.launchApp("whatsapp")
                "হোয়াটসঅ্যাপ খুলছি"
            }

            // ── Facebook ─────────────────────────────────────────────────────
            text.contains("ফেসবুক") || text.contains("facebook") -> {
                phoneController.launchApp("facebook")
                "ফেসবুক খুলছি"
            }

            // ── Settings ─────────────────────────────────────────────────────
            text.contains("সেটিংস") || text.contains("settings") -> {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "সেটিংস খুলছি"
            }

            // ── Greeting ─────────────────────────────────────────────────────
            text.contains("হ্যালো") || text.contains("hello") ||
            text.contains("হাই") || text.contains("hi") ||
            text.contains("ভালো আছ") || text.contains("কেমন আছ") -> {
                "আমি ভালো আছি। আপনাকে কীভাবে সাহায্য করতে পারি?"
            }

            // ── Thank you ────────────────────────────────────────────────────
            text.contains("ধন্যবাদ") || text.contains("thanks") ||
            text.contains("thank you") -> {
                "আপনাকে স্বাগতম!"
            }

            // ── Unknown ──────────────────────────────────────────────────────
            else -> {
                Log.d(TAG, "Unknown command: $text")
                ""  // Stay silent for unrecognised commands to avoid noise
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Extracts the word(s) that follow the first matching keyword in [text].
     * E.g. "ফোন করো রাহেলাকে" with keyword "ফোন করো" → "রাহেলাকে"
     */
    private fun extractNameAfterKeyword(text: String, keywords: List<String>): String {
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx >= 0) {
                val after = text.substring(idx + kw.length).trim()
                // Strip trailing Bengali case suffixes (কে, কে, তে, র, এর, …)
                return after
                    .removePrefix("কে")
                    .trim()
            }
        }
        return ""
    }
}

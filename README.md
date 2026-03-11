# MR.TOM — ভয়েস অ্যাসিস্ট্যান্ট

MR.TOM is an always-on Android voice assistant that gives you full voice control over your phone.
আপনার ফোনের সব কাজ শুধু কথা বলে করুন — MR.TOM সব সময় সক্রিয় থাকে।

---

## ✨ Features / বৈশিষ্ট্য

| বাংলা কমান্ড | English Command | Action |
|---|---|---|
| `ফোন করো <নাম>` | `call <name>` | Make a phone call |
| `মেসেজ করো <নাম>` | `sms <name>` | Open SMS to contact |
| `<অ্যাপ> খোলো` | `open <app>` | Launch any installed app |
| `সময়` | `time` | Read current time aloud |
| `তারিখ` | `date` | Read current date aloud |
| `ব্যাটারি` | `battery` | Read battery percentage |
| `টর্চ / আলো` | `torch / flashlight` | Toggle flashlight |
| `ভলিউম বাড়াও` | `volume up` | Raise media volume |
| `ভলিউম কমাও` | `volume down` | Lower media volume |
| `সাইলেন্ট` | `silent / mute` | Set ringer to silent |
| `রিং` | `ring` | Set ringer to normal |
| `ওয়াইফাই` | `wifi` | Open Wi-Fi settings |
| `ব্লুটুথ` | `bluetooth` | Open Bluetooth settings |
| `ক্যামেরা` | `camera` | Open camera |
| `গান / মিউজিক` | `music / play` | Open music player |
| `গুগল / ব্রাউজার` | `google / browser` | Open browser |
| `ম্যাপ / নেভিগেশন` | `map / navigation` | Open Google Maps |
| `হোয়াটসঅ্যাপ` | `whatsapp` | Open WhatsApp |
| `ইউটিউব` | `youtube` | Open YouTube |
| `ফেসবুক` | `facebook` | Open Facebook |
| `সেটিংস` | `settings` | Open Settings |

---

## 🚀 Getting Started

1. **Build** the project with Android Studio (SDK 26+).
2. **Install** the APK on your Android device.
3. Tap **"শুরু করুন"** — grant all requested permissions.
4. Tap **"ব্যাটারি অপ্টিমাইজেশন বন্ধ করুন"** so Android does not kill the service.
5. MR.TOM is now always listening. Just speak!

---

## 🏗️ Architecture

```
MR.TOM/
└── app/src/main/
    ├── AndroidManifest.xml         # Permissions + component declarations
    └── java/com/mrtom/assistant/
        ├── MainActivity.kt         # UI — permission requests, start/stop
        ├── AssistantService.kt     # Foreground service — always-on loop
        ├── VoiceCommandProcessor.kt# Maps speech → action, returns TTS reply
        ├── PhoneController.kt      # Calls, SMS, apps, volume, torch …
        └── BootReceiver.kt         # Auto-start after reboot
```

## 🔒 Permissions Required

- `RECORD_AUDIO` — microphone for voice recognition  
- `CALL_PHONE` / `READ_PHONE_STATE` — make calls  
- `SEND_SMS` / `READ_SMS` — send messages  
- `READ_CONTACTS` — look up contact names  
- `ACCESS_FINE_LOCATION` — location / navigation commands  
- `CAMERA` — flashlight & camera app  
- `FOREGROUND_SERVICE` / `WAKE_LOCK` — stay alive in background  
- `RECEIVE_BOOT_COMPLETED` — auto-start after reboot  

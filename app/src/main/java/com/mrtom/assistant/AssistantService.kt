package com.mrtom.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * AssistantService — Foreground service that keeps MR.TOM alive at all times.
 *
 * Runs a continuous voice-recognition loop:
 *   listen → recognise → process command → speak reply → listen again
 *
 * The service is started as a foreground service (with a persistent notification)
 * so Android will not kill it in the background.
 */
class AssistantService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AssistantService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mrtom_assistant_channel"
        private const val RESTART_DELAY_MS = 1500L
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isListening = false

    private lateinit var commandProcessor: VoiceCommandProcessor
    private lateinit var phoneController: PhoneController

    // ---------------------------------------------------------------------------
    // Service lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("MR.TOM চালু আছে — শুনছি…"))

        phoneController = PhoneController(this)
        commandProcessor = VoiceCommandProcessor(this, phoneController)
        tts = TextToSpeech(this, this)

        Log.i(TAG, "AssistantService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")
        // START_STICKY ensures Android restarts the service if it is killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        tts?.stop()
        tts?.shutdown()
        Log.i(TAG, "AssistantService destroyed")
    }

    // ---------------------------------------------------------------------------
    // TTS initialisation
    // ---------------------------------------------------------------------------

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Prefer Bengali; fall back to English
            val result = tts?.setLanguage(Locale("bn", "BD"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                tts?.language = Locale.ENGLISH
                ttsReady = true
            }
            Log.i(TAG, "TTS ready (Bengali=$ttsReady)")
            speak("MR.TOM সক্রিয়। আমি শুনছি।")
            startListening()
        } else {
            Log.e(TAG, "TTS initialisation failed")
            startListening()
        }
    }

    // ---------------------------------------------------------------------------
    // Voice recognition
    // ---------------------------------------------------------------------------

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        if (isListening) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_ALSO_RECOGNIZE_SPEECH, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
        updateNotification("MR.TOM শুনছে…")
        Log.d(TAG, "Listening started")
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    /** Restart the recognition loop after a short delay. */
    private fun restartListening() {
        isListening = false
        stopListening()
        android.os.Handler(mainLooper).postDelayed({
            startListening()
        }, RESTART_DELAY_MS)
    }

    // ---------------------------------------------------------------------------
    // RecognitionListener
    // ---------------------------------------------------------------------------

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
            updateNotification("MR.TOM শুনছে…")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            isListening = false
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা মেলেনি"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "সময় শেষ"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "রিকগনাইজার ব্যস্ত"
                SpeechRecognizer.ERROR_AUDIO -> "অডিও সমস্যা"
                SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সমস্যা"
                else -> "অজানা ত্রুটি: $error"
            }
            Log.w(TAG, "Recognition error: $msg")
            restartListening()
        }

        override fun onResults(results: android.os.Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val command = matches[0]
                Log.i(TAG, "Recognised: $command")
                handleCommand(command)
            }
            restartListening()
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {}

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    // ---------------------------------------------------------------------------
    // Command handling
    // ---------------------------------------------------------------------------

    private fun handleCommand(command: String) {
        updateNotification("কমান্ড: $command")
        val reply = commandProcessor.process(command)
        if (reply.isNotBlank()) {
            speak(reply)
        }
    }

    // ---------------------------------------------------------------------------
    // TTS helper
    // ---------------------------------------------------------------------------

    fun speak(text: String) {
        Log.d(TAG, "TTS: $text")
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mrtom_${System.currentTimeMillis()}")
        }
    }

    // ---------------------------------------------------------------------------
    // Notification helpers
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MR.TOM Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MR.TOM ভয়েস অ্যাসিস্ট্যান্ট সক্রিয় থাকার বিজ্ঞপ্তি"
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MR.TOM")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(content))
    }
}

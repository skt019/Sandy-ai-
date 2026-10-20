package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.sin

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private var onSpeechResultListener: ((String) -> Unit)? = null
    private var speechPulseJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        mainHandler.post {
            initSpeechRecognizer()
            textToSpeech = TextToSpeech(context.applicationContext, this)
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceManager)
            }
        }
    }

    fun setOnSpeechResult(listener: (String) -> Unit) {
        onSpeechResultListener = listener
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        textToSpeech?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch
        textToSpeech?.setPitch(pitch)
    }

    fun startListening() {
        mainHandler.post {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            stopSpeaking()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                _partialTranscript.value = ""
                _voiceState.value = VoiceState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
            _audioLevel.value = 0f
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!_autoSpeak.value || !isTtsInitialized) {
            onDone?.invoke()
            return
        }

        stopSpeaking()

        // Clean markdown annotations for speech synthesis
        val cleanText = cleanMarkdownForSpeech(text)
        if (cleanText.isBlank()) {
            onDone?.invoke()
            return
        }

        _voiceState.value = VoiceState.SPEAKING
        val utteranceId = UUID.randomUUID().toString()

        startSimulatedVoiceWaveform()

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _voiceState.value = VoiceState.SPEAKING
            }

            override fun onDone(id: String?) {
                stopSimulatedVoiceWaveform()
                _voiceState.value = VoiceState.IDLE
                _audioLevel.value = 0f
                onDone?.invoke()
            }

            override fun onError(id: String?) {
                stopSimulatedVoiceWaveform()
                _voiceState.value = VoiceState.IDLE
                _audioLevel.value = 0f
                onDone?.invoke()
            }
        })

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        stopSimulatedVoiceWaveform()
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
        _audioLevel.value = 0f
    }

    private fun startSimulatedVoiceWaveform() {
        speechPulseJob?.cancel()
        speechPulseJob = scope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive && _voiceState.value == VoiceState.SPEAKING) {
                val amp = ((sin(step) + 1f) / 2f) * 0.6f + 0.2f
                _audioLevel.value = amp
                step += 0.35f
                delay(60)
            }
            _audioLevel.value = 0f
        }
    }

    private fun stopSimulatedVoiceWaveform() {
        speechPulseJob?.cancel()
        speechPulseJob = null
    }

    private fun cleanMarkdownForSpeech(text: String): String {
        return text
            .replace(Regex("```[a-zA-Z]*\\n([\\s\\S]*?)\\n```"), "code block omitted.")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            .replace(Regex("[\\$\\#\\_]"), "")
            .trim()
    }

    // SpeechRecognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value == VoiceState.LISTENING) {
            // Normalize dB (typically -2dB to 10dB) to 0f..1f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
            _audioLevel.value = normalized
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _voiceState.value = VoiceState.PROCESSING
        _audioLevel.value = 0f
    }

    override fun onError(error: Int) {
        _voiceState.value = VoiceState.IDLE
        _audioLevel.value = 0f
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        _partialTranscript.value = text
        _voiceState.value = VoiceState.IDLE
        _audioLevel.value = 0f
        if (text.isNotBlank()) {
            onSpeechResultListener?.invoke(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _partialTranscript.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // TextToSpeech.OnInitListener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    tts.setSpeechRate(_speechRate.value)
                    tts.setPitch(_pitch.value)
                }
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            stopSimulatedVoiceWaveform()
        }
    }
}

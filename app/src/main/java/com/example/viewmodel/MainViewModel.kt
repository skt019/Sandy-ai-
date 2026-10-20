package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.PhoneActionHandler
import com.example.actions.SandyAction
import com.example.actions.SandyActionParser
import com.example.data.files.FileProcessor
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.NoteEntity
import com.example.data.local.SandyInteractionEntity
import com.example.data.local.UploadedFileEntity
import com.example.data.model.InferenceParams
import com.example.data.model.InferenceStats
import com.example.data.model.ModelInfo
import com.example.data.repository.ModelRepository
import com.example.engine.GenerationEvent
import com.example.engine.LocalInferenceEngine
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    CHAT,
    VOICE_CHAT,
    MODEL_MANAGER,
    SETTINGS
}

data class PersonaPreset(
    val name: String,
    val icon: String,
    val description: String,
    val systemPrompt: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val noteDao = db.noteDao()
    private val interactionDao = db.interactionDao()
    private val uploadedFileDao = db.uploadedFileDao()

    val modelRepository = ModelRepository(application)
    val inferenceEngine = LocalInferenceEngine()
    val voiceManager = VoiceManager(application, viewModelScope)

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active session
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    val allSessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    // Sandy Notes collection
    val notes: StateFlow<List<NoteEntity>> = noteDao.getAllNotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sandy Interaction & Voice Command History collection
    val interactionHistory: StateFlow<List<SandyInteractionEntity>> = interactionDao.getAllInteractionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Uploaded Files (PDF / JPG) and related stored data
    val uploadedFiles: StateFlow<List<UploadedFileEntity>> = uploadedFileDao.getAllFilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active pending phone action (WhatsApp, Call, SMS, YouTube, Alarm, etc.)
    private val _pendingAction = MutableStateFlow<SandyAction?>(null)
    val pendingAction: StateFlow<SandyAction?> = _pendingAction.asStateFlow()

    // Captured photo for visual Q&A
    private val _capturedPhoto = MutableStateFlow<Bitmap?>(null)
    val capturedPhoto: StateFlow<Bitmap?> = _capturedPhoto.asStateFlow()

    // Streaming response during active generation
    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    // Telemetry & stats
    private val _currentStats = MutableStateFlow(InferenceStats())
    val currentStats: StateFlow<InferenceStats> = _currentStats.asStateFlow()

    private val _showTelemetryBar = MutableStateFlow(true)
    val showTelemetryBar: StateFlow<Boolean> = _showTelemetryBar.asStateFlow()

    // Default Sandy system prompt
    val defaultSandyPrompt = "You are Sandy, a smart, friendly, and capable personal AI assistant running locally on this Android device. " +
            "You can handle WhatsApp chats, phone calls, SMS, YouTube videos, alarms, notes, and camera visual questions."

    // Inference tuning settings
    private val _params = MutableStateFlow(InferenceParams(systemPrompt = defaultSandyPrompt))
    val params: StateFlow<InferenceParams> = _params.asStateFlow()

    // Continuous voice chat mode toggle
    private val _continuousVoiceMode = MutableStateFlow(false)
    val continuousVoiceMode: StateFlow<Boolean> = _continuousVoiceMode.asStateFlow()

    // Selected Persona
    val personaPresets = listOf(
        PersonaPreset(
            name = "Sandy (All Actions)",
            icon = "✨",
            description = "Handles calls, WhatsApp, alarms, notes, and camera",
            systemPrompt = defaultSandyPrompt
        ),
        PersonaPreset(
            name = "Sandy Voice Companion",
            icon = "🎙️",
            description = "Natural conversational speech for hands-free voice chat",
            systemPrompt = "You are Sandy, a friendly voice assistant. Keep answers natural, direct, and conversational without markdown."
        ),
        PersonaPreset(
            name = "Sandy Code Expert",
            icon = "💻",
            description = "Specialized in clean Kotlin, algorithms, and debugging",
            systemPrompt = "You are Sandy, an expert software engineer. Provide clean, idiomatic code examples with concise technical explanations."
        ),
        PersonaPreset(
            name = "Sandy Reasoning",
            icon = "🧠",
            description = "Step-by-step analytical problem solving and math",
            systemPrompt = "You are Sandy, a logical reasoning assistant. Break problems down step by step and verify computations methodically."
        )
    )

    private val _selectedPersona = MutableStateFlow(personaPresets[0])
    val selectedPersona: StateFlow<PersonaPreset> = _selectedPersona.asStateFlow()

    private var generationJob: Job? = null
    private var messageCollectionJob: Job? = null

    init {
        // Setup initial chat session
        viewModelScope.launch {
            val sessions = chatDao.getAllSessionsFlow().firstOrNull() ?: emptyList()
            if (sessions.isNotEmpty()) {
                switchSession(sessions.first().id)
            } else {
                createNewSession()
            }
        }

        // Seed initial Sandy interactions if empty
        viewModelScope.launch {
            val existing = interactionDao.getAllInteractionsFlow().firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                val now = System.currentTimeMillis()
                interactionDao.insertInteraction(
                    SandyInteractionEntity(
                        command = "Sandy, what can you do?",
                        actionType = "VOICE",
                        actionSummary = "Overview of phone calls, WhatsApp, alarms, notes, YouTube & camera Q&A",
                        isVoice = true,
                        status = "COMPLETED",
                        timestamp = now - 3600000L
                    )
                )
                interactionDao.insertInteraction(
                    SandyInteractionEntity(
                        command = "Sandy, set an alarm for 7:00 AM",
                        actionType = "ALARM",
                        actionSummary = "Alarm set for 7:00 AM",
                        isVoice = true,
                        status = "COMPLETED",
                        timestamp = now - 1800000L
                    )
                )
                interactionDao.insertInteraction(
                    SandyInteractionEntity(
                        command = "Sandy, note buy groceries and fresh milk",
                        actionType = "NOTE",
                        actionSummary = "Saved note: \"buy groceries and fresh milk\"",
                        isVoice = false,
                        status = "COMPLETED",
                        timestamp = now - 600000L
                    )
                )
            }
        }

        // Seed initial sample PDF & JPG files in Room database if empty
        viewModelScope.launch {
            val existingFiles = uploadedFileDao.getAllFiles()
            if (existingFiles.isEmpty()) {
                uploadedFileDao.insertFile(FileProcessor.createSamplePdf())
                uploadedFileDao.insertFile(FileProcessor.createSampleJpg())
            }
        }

        // Voice speech callback
        voiceManager.setOnSpeechResult { transcript ->
            if (transcript.isNotBlank()) {
                sendUserPrompt(transcript, isVoiceInput = true)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun toggleTelemetryBar() {
        _showTelemetryBar.update { !it }
    }

    fun toggleContinuousVoice() {
        _continuousVoiceMode.update { !it }
    }

    fun selectPersona(persona: PersonaPreset) {
        _selectedPersona.value = persona
        _params.update { it.copy(systemPrompt = persona.systemPrompt) }
    }

    fun updateParams(newParams: InferenceParams) {
        _params.value = newParams
    }

    fun createNewSession(customTitle: String? = null) {
        viewModelScope.launch {
            val activeModelName = modelRepository.activeModel.value?.name ?: "Local GGUF"
            val title = customTitle ?: "Sandy Chat ${allSessions.value.size + 1}"
            val newSession = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                modelName = activeModelName,
                systemPrompt = _params.value.systemPrompt
            )
            chatDao.insertSession(newSession)
            switchSession(newSession.id)
        }
    }

    fun switchSession(sessionId: String) {
        _activeSessionId.value = sessionId
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatDao.getMessagesForSessionFlow(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun clearCurrentSession() {
        val sId = _activeSessionId.value ?: return
        viewModelScope.launch {
            chatDao.clearMessagesForSession(sId)
            _messages.value = emptyList()
            _streamingMessage.value = null
            _pendingAction.value = null
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteSessionById(sessionId)
            if (_activeSessionId.value == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    switchSession(remaining.first().id)
                } else {
                    createNewSession()
                }
            }
        }
    }

    // Sandy Notes Operations
    fun addNote(content: String) {
        viewModelScope.launch {
            val title = if (content.length > 25) content.take(25) + "..." else content
            noteDao.insertNote(NoteEntity(title = title, content = content))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteDao.deleteNoteById(id)
        }
    }

    // Camera Visual Q&A
    fun setCapturedPhoto(bitmap: Bitmap?) {
        _capturedPhoto.value = bitmap
    }

    fun askSandyWithPhoto(bitmap: Bitmap, question: String) {
        _capturedPhoto.value = bitmap
        sendUserPrompt("[Visual Analysis from Camera] $question")
    }

    // Sandy Interaction History Operations
    fun logInteraction(command: String, actionType: String, actionSummary: String, isVoice: Boolean) {
        viewModelScope.launch {
            interactionDao.insertInteraction(
                SandyInteractionEntity(
                    command = command,
                    actionType = actionType,
                    actionSummary = actionSummary,
                    isVoice = isVoice
                )
            )
        }
    }

    fun deleteInteraction(id: String) {
        viewModelScope.launch {
            interactionDao.deleteInteractionById(id)
        }
    }

    fun clearAllInteractions() {
        viewModelScope.launch {
            interactionDao.clearAllInteractions()
        }
    }

    fun sendUserPrompt(promptText: String, isVoiceInput: Boolean = false) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank()) return

        val isVoice = isVoiceInput || _currentScreen.value == AppScreen.VOICE_CHAT
        val sessionId = _activeSessionId.value ?: return

        // 1. Detect device actions (WhatsApp, Call, SMS, YouTube, Alarm, Note, Camera)
        val detectedAction = SandyActionParser.parseAction(trimmed)
        _pendingAction.value = detectedAction

        // If it's a note action, automatically persist it into Sandy's notes database
        if (detectedAction is SandyAction.Note) {
            addNote(detectedAction.content)
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = trimmed
            )
            chatDao.insertMessage(userMsg)

            _streamingMessage.value = ""
            _currentStats.value = _currentStats.value.copy(isGenerating = true)

            // If action is detected, synthesize direct Sandy confirmation response
            if (detectedAction != null && !trimmed.contains("[Visual Analysis from Camera]")) {
                val actionResponse = when (detectedAction) {
                    is SandyAction.WhatsApp -> {
                        val recipient = detectedAction.recipient ?: "your contact"
                        "Sandy here! I've prepared your WhatsApp message for **$recipient**: *\"${detectedAction.message}\"*. Tap below to open WhatsApp and send it."
                    }
                    is SandyAction.Call -> {
                        val target = detectedAction.contactName ?: detectedAction.phoneNumber
                        "Calling **$target** now. Tap the dial button below to open your phone dialer."
                    }
                    is SandyAction.Sms -> {
                        "Preparing SMS to **${detectedAction.recipient}**: *\"${detectedAction.message}\"*. Tap below to open your messaging app."
                    }
                    is SandyAction.YouTube -> {
                        "Opening YouTube to play **\"${detectedAction.query}\"** for you! 🎬"
                    }
                    is SandyAction.Alarm -> {
                        val amPm = if (detectedAction.hour >= 12) "PM" else "AM"
                        val displayH = if (detectedAction.hour == 0) 12 else if (detectedAction.hour > 12) detectedAction.hour - 12 else detectedAction.hour
                        val displayM = String.format("%02d", detectedAction.minute)
                        "Setting your alarm for **$displayH:$displayM $amPm**. Confirm below."
                    }
                    is SandyAction.Timer -> {
                        "Starting a timer for **${detectedAction.seconds / 60} minutes**."
                    }
                    is SandyAction.Note -> {
                        "I've noted that down for you in Sandy Notes: *\"${detectedAction.content}\"* 📝"
                    }
                    is SandyAction.CameraQuestion -> {
                        "Opening camera for Sandy visual analysis. Point your camera at what you want me to look at!"
                    }
                    is SandyAction.FileQuery -> {
                        generateFileInfoResponse(detectedAction.query, detectedAction.targetFileType)
                    }
                    is SandyAction.WhatsAppStatus -> {
                        "Sandy here! I've prepared your WhatsApp status: *\"${detectedAction.statusText}\"*. Tap below to share it directly to your WhatsApp status."
                    }
                    is SandyAction.InstagramMessage -> {
                        val recipientClean = detectedAction.recipient.trim().removePrefix("@")
                        "Sandy here! Ready to send a personal message to **@$recipientClean** on Instagram: *\"${detectedAction.message}\"*. Tap below to open Instagram Direct."
                    }
                    is SandyAction.InstagramPost -> {
                        "Sandy here! Ready to post on Instagram with caption: *\"${detectedAction.caption}\"*. Tap below to open Instagram and share your post."
                    }
                }

                // Simulate token streaming for Sandy confirmation
                val tokens = actionResponse.split(" ")
                val acc = StringBuilder()
                for ((idx, tok) in tokens.withIndex()) {
                    kotlinx.coroutines.delay(20)
                    acc.append(tok).append(if (idx < tokens.size - 1) " " else "")
                    _streamingMessage.value = acc.toString()
                }

                _streamingMessage.value = null
                val finalStats = _currentStats.value.copy(isGenerating = false, tokenCount = tokens.size)
                _currentStats.value = finalStats

                val (actionType, actionSummary) = when (detectedAction) {
                    is SandyAction.WhatsApp -> Pair("WHATSAPP", "WhatsApp to ${detectedAction.recipient ?: "contact"}: \"${detectedAction.message}\"")
                    is SandyAction.Call -> Pair("CALL", "Call ${detectedAction.contactName ?: detectedAction.phoneNumber}")
                    is SandyAction.Sms -> Pair("SMS", "SMS to ${detectedAction.recipient}: \"${detectedAction.message}\"")
                    is SandyAction.YouTube -> Pair("YOUTUBE", "Play \"${detectedAction.query}\" on YouTube")
                    is SandyAction.Alarm -> Pair("ALARM", "Alarm set for ${detectedAction.hour}:${String.format("%02d", detectedAction.minute)}")
                    is SandyAction.Timer -> Pair("TIMER", "Timer for ${detectedAction.seconds / 60}m")
                    is SandyAction.Note -> Pair("NOTE", "Saved note: \"${detectedAction.content}\"")
                    is SandyAction.CameraQuestion -> Pair("CAMERA_QA", "Camera Q&A: ${detectedAction.question}")
                    is SandyAction.FileQuery -> Pair("FILE_INFO", "Queried file: ${detectedAction.query.take(35)}")
                    is SandyAction.WhatsAppStatus -> Pair("WHATSAPP_STATUS", "Status: \"${detectedAction.statusText}\"")
                    is SandyAction.InstagramMessage -> Pair("INSTAGRAM_DM", "DM to @${detectedAction.recipient.removePrefix("@")}: \"${detectedAction.message}\"")
                    is SandyAction.InstagramPost -> Pair("INSTAGRAM_POST", "Post: \"${detectedAction.caption}\"")
                }
                logInteraction(trimmed, actionType, actionSummary, isVoice)

                val assistantMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = actionResponse,
                    tokensPerSecond = 35.0f,
                    generationTimeMs = (tokens.size * 20).toLong(),
                    tokenCount = tokens.size
                )
                chatDao.insertMessage(assistantMsg)

                if (_currentScreen.value == AppScreen.VOICE_CHAT || voiceManager.autoSpeak.value) {
                    voiceManager.speak(actionResponse) {
                        if (_continuousVoiceMode.value && _currentScreen.value == AppScreen.VOICE_CHAT) {
                            voiceManager.startListening()
                        }
                    }
                }
                return@launch
            }

            // Standard on-device inference via LocalInferenceEngine
            val activeModel = modelRepository.activeModel.value
            val history = _messages.value.map { it.role to it.content }
            val formattedPrompt = inferenceEngine.formatPrompt(
                model = activeModel,
                systemPrompt = _params.value.systemPrompt,
                conversationHistory = history,
                newPrompt = trimmed
            )

            val accumulated = StringBuilder()

            inferenceEngine.generateStream(activeModel, formattedPrompt, _params.value).collect { event ->
                when (event) {
                    is GenerationEvent.Token -> {
                        accumulated.append(event.text)
                        _streamingMessage.value = accumulated.toString()
                        _currentStats.value = event.stats
                    }
                    is GenerationEvent.Complete -> {
                        _streamingMessage.value = null
                        _currentStats.value = event.stats

                        val assistantMsg = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = event.fullText,
                            tokensPerSecond = event.stats.tokensPerSecond,
                            generationTimeMs = event.stats.totalGenerationTimeMs,
                            tokenCount = event.stats.tokenCount
                        )
                        chatDao.insertMessage(assistantMsg)

                        if (detectedAction == null) {
                            val actionType = if (trimmed.contains("[Visual Analysis from Camera]")) {
                                "CAMERA_QA"
                            } else if (isVoice) {
                                "VOICE"
                            } else {
                                "CONVERSATION"
                            }
                            val summary = if (event.fullText.length > 80) event.fullText.take(80) + "..." else event.fullText
                            logInteraction(trimmed, actionType, summary, isVoice)
                        }

                        if (_currentScreen.value == AppScreen.VOICE_CHAT || voiceManager.autoSpeak.value) {
                            voiceManager.speak(event.fullText) {
                                if (_continuousVoiceMode.value && _currentScreen.value == AppScreen.VOICE_CHAT) {
                                    voiceManager.startListening()
                                }
                            }
                        }
                    }
                    is GenerationEvent.Error -> {
                        _streamingMessage.value = null
                        _currentStats.value = _currentStats.value.copy(isGenerating = false)
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        inferenceEngine.cancelGeneration()
        generationJob?.cancel()
        generationJob = null
        val currentText = _streamingMessage.value
        val sessionId = _activeSessionId.value
        if (!currentText.isNullOrBlank() && sessionId != null) {
            viewModelScope.launch {
                val assistantMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "$currentText ⏹️ (Stopped)",
                    tokensPerSecond = _currentStats.value.tokensPerSecond,
                    generationTimeMs = _currentStats.value.totalGenerationTimeMs,
                    tokenCount = _currentStats.value.tokenCount
                )
                chatDao.insertMessage(assistantMsg)
                _streamingMessage.value = null
                _currentStats.value = _currentStats.value.copy(isGenerating = false)
            }
        } else {
            _streamingMessage.value = null
            _currentStats.value = _currentStats.value.copy(isGenerating = false)
        }
    }

    fun regenerateLastMessage() {
        val lastUserMsg = _messages.value.lastOrNull { it.role == "user" } ?: return
        sendUserPrompt(lastUserMsg.content)
    }

    fun importCustomGgufFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            modelRepository.importCustomGguf(uri, fileName)
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.downloadModel(modelId)
        }
    }

    private suspend fun generateFileInfoResponse(query: String, targetFileType: String?): String {
        val files = uploadedFileDao.getAllFiles()
        if (files.isEmpty()) {
            return "📂 **Sandy File Assistant**:\n\n" +
                    "No files have been uploaded yet. Tap the **Upload File (PDF/JPG)** button or select **+ Sample Invoice/Receipt** to store and analyze documents in the local Room database!"
        }

        val target = if (targetFileType != null) {
            files.firstOrNull { it.fileType.equals(targetFileType, ignoreCase = true) }
        } else {
            val queryLower = query.lowercase()
            files.firstOrNull { file ->
                val nameLower = file.fileName.lowercase()
                val base = nameLower.substringBeforeLast(".")
                queryLower.contains(nameLower) || queryLower.contains(base) ||
                        (queryLower.contains("invoice") && nameLower.contains("invoice")) ||
                        (queryLower.contains("receipt") && nameLower.contains("receipt"))
            } ?: files.firstOrNull()
        }

        if (target == null) {
            return "Sandy here! I could not locate a matching file. Currently you have **${files.size} document(s)** stored in the local Room database: " +
                    files.joinToString(", ") { "${it.fileName} (${it.fileType})" }
        }

        val typeEmoji = if (target.fileType == "PDF") "📄" else "🖼️"
        val dimensionDetail = if (target.fileType == "PDF") "${target.pageCount} page(s), ${target.dimensions}" else "Resolution: ${target.dimensions ?: "HD"}"

        return "$typeEmoji **Sandy File Information**: **${target.fileName}**\n\n" +
                "• **Format & Size**: ${target.fileType} • ${target.fileSizeFormatted}\n" +
                "• **Pages / Image Resolution**: $dimensionDetail\n" +
                "• **Database Record**: Room Table `uploaded_files`, Status: **${target.analysisStatus}**\n\n" +
                "📋 **Document Summary**:\n${target.summary}\n\n" +
                "💡 **Stored Key Insights**:\n${target.keyInsights}\n\n" +
                "💬 *All related data was extracted and saved securely on your device. You can ask me to extract specific figures, summarize sections, or compare files anytime!*"
    }

    fun uploadFile(uri: Uri, explicitType: String? = null) {
        viewModelScope.launch {
            try {
                val entity = FileProcessor.processAndStoreFile(getApplication(), uri, explicitType)
                uploadedFileDao.insertFile(entity)

                val summaryMsg = "📄 **New File Uploaded & Stored**: **${entity.fileName}** (${entity.fileType}, ${entity.fileSizeFormatted})\n\n" +
                        "• **Executive Summary**: ${entity.summary}\n" +
                        "• **Stored Details**: ${if (entity.fileType == "PDF") "${entity.pageCount} Pages (${entity.dimensions})" else "Resolution: " + (entity.dimensions ?: "HD")}\n\n" +
                        "💾 *All related metadata and insights have been stored in the local Room database.* You can ask: **\"Tell me about ${entity.fileName}\"** or **\"Summarize this ${entity.fileType}\"** anytime!"

                val sessionId = _activeSessionId.value
                if (sessionId != null) {
                    val msg = ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = summaryMsg,
                        tokensPerSecond = 35f,
                        generationTimeMs = 120,
                        tokenCount = 50
                    )
                    chatDao.insertMessage(msg)
                }

                logInteraction("Uploaded ${entity.fileName}", "FILE_UPLOAD", "Stored ${entity.fileType}: ${entity.fileName}", isVoice = false)
            } catch (e: Exception) {
                val sessionId = _activeSessionId.value
                if (sessionId != null) {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = "Could not process file: ${e.localizedMessage ?: "Unknown error"}",
                            tokensPerSecond = 20f,
                            generationTimeMs = 50,
                            tokenCount = 10
                        )
                    )
                }
            }
        }
    }

    fun uploadSampleFile(sampleType: String) {
        viewModelScope.launch {
            val entity = if (sampleType == "JPG_RECEIPT") FileProcessor.createSampleJpg() else FileProcessor.createSamplePdf()
            uploadedFileDao.insertFile(entity)

            val summaryMsg = "📄 **Sample ${entity.fileType} Document Loaded**: **${entity.fileName}** (${entity.fileSizeFormatted})\n\n" +
                    "• **Summary**: ${entity.summary}\n" +
                    "• **Key Insights**:\n${entity.keyInsights}\n\n" +
                    "💾 *Saved to local Room database.* You can now ask: **\"Tell me about the ${entity.fileType} file\"**!"

            val sessionId = _activeSessionId.value
            if (sessionId != null) {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "assistant",
                        content = summaryMsg,
                        tokensPerSecond = 35f,
                        generationTimeMs = 100,
                        tokenCount = 45
                    )
                )
            }
            logInteraction("Loaded sample ${entity.fileName}", "FILE_UPLOAD", "Stored sample ${entity.fileType}: ${entity.fileName}", isVoice = false)
        }
    }

    fun deleteUploadedFile(id: Long) {
        viewModelScope.launch {
            uploadedFileDao.deleteFileById(id)
        }
    }

    fun clearAllUploadedFiles() {
        viewModelScope.launch {
            uploadedFileDao.clearAllFiles()
        }
    }

    fun askAboutUploadedFile(file: UploadedFileEntity) {
        sendUserPrompt("Tell me about the file ${file.fileName}", isVoiceInput = false)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

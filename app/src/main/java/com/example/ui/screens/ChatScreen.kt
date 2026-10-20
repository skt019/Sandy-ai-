package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.actions.SandyAction
import com.example.data.local.ChatMessageEntity
import com.example.data.model.InferenceStats
import com.example.data.model.ModelStatus
import com.example.ui.components.CameraAssistantDialog
import com.example.ui.components.GeminiSparkleStar
import com.example.ui.components.MarkdownText
import com.example.ui.components.RealtimeSpeechRecordingBar
import com.example.ui.components.SandyActionCard
import com.example.ui.components.SandyNotesDialog
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PersonaPreset
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val allModels by viewModel.modelRepository.models.collectAsState()
    val activeModel by viewModel.modelRepository.activeModel.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()
    val showTelemetry by viewModel.showTelemetryBar.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioLevel by viewModel.voiceManager.audioLevel.collectAsState()
    val partialTranscript by viewModel.voiceManager.partialTranscript.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val capturedPhoto by viewModel.capturedPhoto.collectAsState()

    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var tempCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Audio permission launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.voiceManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission required for Sandy", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher & capture
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            tempCameraBitmap = bitmap
            showCameraDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraPreviewLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required for Sandy to analyze photos", Toast.LENGTH_SHORT).show()
        }
    }

    // SAF Document Picker for .gguf files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported_model.gguf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.importCustomGgufFile(uri, fileName)
            Toast.makeText(context, "Sandy loaded $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to bottom on new messages or streaming tokens
    LaunchedEffect(messages.size, streamingMessage, pendingAction) {
        val totalItems = messages.size + if (streamingMessage != null) 1 else 0 + if (pendingAction != null) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        // Sandy Model & Status Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .clickable { showModelDropdown = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("model_selector_top_chip")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sandy (${activeModel?.name ?: "Local"})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Model",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Model selection dropdown
                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false }
                        ) {
                            val installed = allModels.filter { it.status == ModelStatus.READY || it.status == ModelStatus.LOADED }
                            for (m in installed) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(m.name, fontWeight = FontWeight.SemiBold)
                                                Text("${m.quantization} • ${m.parameterCount}", fontSize = 11.sp, color = NeonCyan)
                                            }
                                            if (m.id == activeModel?.id) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.modelRepository.selectModel(m.id)
                                        showModelDropdown = false
                                        Toast.makeText(context, "Sandy loaded ${m.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Import .GGUF File...", color = NeonPurple, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    showModelDropdown = false
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Model Hub Catalog")
                                    }
                                },
                                onClick = {
                                    showModelDropdown = false
                                    viewModel.navigateTo(AppScreen.MODEL_MANAGER)
                                }
                            )
                        }
                    }
                },
                actions = {
                    // Notes Button
                    IconButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.testTag("sandy_notes_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Sandy Notes",
                            tint = NeonPurple
                        )
                    }

                    // Telemetry Button
                    IconButton(
                        onClick = { viewModel.toggleTelemetryBar() },
                        modifier = Modifier.testTag("telemetry_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Toggle Telemetry",
                            tint = if (showTelemetry) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Menu Button
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("chat_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Sandy Chat") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                viewModel.createNewSession()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sandy Notes (${notes.size})") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            onClick = {
                                showNotesDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Model Hub & Storage") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                            onClick = {
                                viewModel.navigateTo(AppScreen.MODEL_MANAGER)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Inference Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                viewModel.navigateTo(AppScreen.SETTINGS)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Messages") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                viewModel.clearCurrentSession()
                                showMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBackground
                )
            )
        },
        containerColor = CyberBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Hardware Telemetry Bar
            AnimatedVisibility(visible = showTelemetry) {
                TelemetryBar(
                    activeModel = activeModel,
                    stats = currentStats,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Persona Selector Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.personaPresets) { persona ->
                    PersonaChip(
                        persona = persona,
                        isSelected = persona.name == selectedPersona.name,
                        onClick = { viewModel.selectPersona(persona) }
                    )
                }
            }

            // Messages List / Empty Hero View
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                if (messages.isEmpty() && streamingMessage == null) {
                    item {
                        SandyHeroEmptyView(
                            activeModelName = activeModel?.name ?: "SmolLM2 135M",
                            quantization = activeModel?.quantization ?: "Q4_K_M",
                            onVoiceClick = { viewModel.navigateTo(AppScreen.VOICE_CHAT) },
                            onCameraClick = {
                                if (!hasCameraPermission) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    cameraPreviewLauncher.launch(null)
                                }
                            },
                            onQuickPrompt = { prompt ->
                                viewModel.sendUserPrompt(prompt)
                            }
                        )
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        capturedPhoto = if (msg.role == "user" && msg.content.contains("[Visual Analysis from Camera]")) capturedPhoto else null,
                        onSpeak = { viewModel.voiceManager.speak(msg.content) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("chat", msg.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onRegenerate = { viewModel.regenerateLastMessage() }
                    )
                }

                // Interactive Action Card for detected phone actions
                if (pendingAction != null) {
                    item {
                        SandyActionCard(
                            action = pendingAction!!,
                            onExecuteOverride = if (pendingAction is SandyAction.CameraQuestion) {
                                {
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        cameraPreviewLauncher.launch(null)
                                    }
                                }
                            } else null
                        )
                    }
                }

                // Active Streaming Bubble
                if (streamingMessage != null) {
                    item {
                        StreamingBubble(
                            content = streamingMessage.orEmpty(),
                            stats = currentStats,
                            onStop = { viewModel.stopGeneration() }
                        )
                    }
                }
            }

            // Bottom Input Bar with Real-time Waveform, Mic, Camera, and '+' file importer
            ChatInputPillContainer(
                text = inputPrompt,
                isGenerating = currentStats.isGenerating,
                voiceState = voiceState,
                audioLevel = audioLevel,
                partialTranscript = partialTranscript,
                onTextChange = { inputPrompt = it },
                onSend = {
                    if (inputPrompt.isNotBlank()) {
                        viewModel.sendUserPrompt(inputPrompt)
                        inputPrompt = ""
                    }
                },
                onStop = { viewModel.stopGeneration() },
                onStartSpeech = {
                    if (!hasAudioPermission) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.voiceManager.startListening()
                    }
                },
                onCancelSpeech = {
                    viewModel.voiceManager.stopListening()
                },
                onSendSpeech = {
                    val speechText = partialTranscript
                    viewModel.voiceManager.stopListening()
                    if (speechText.isNotBlank()) {
                        viewModel.sendUserPrompt(speechText)
                    }
                },
                onCameraClick = {
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        cameraPreviewLauncher.launch(null)
                    }
                },
                onPickGgufFile = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onOpenLiveVoice = {
                    viewModel.navigateTo(AppScreen.VOICE_CHAT)
                }
            )
        }
    }

    // Camera Assistant Dialog
    if (showCameraDialog) {
        CameraAssistantDialog(
            capturedBitmap = tempCameraBitmap,
            onRetake = {
                if (!hasCameraPermission) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    cameraPreviewLauncher.launch(null)
                }
            },
            onDismiss = {
                showCameraDialog = false
            },
            onAskSandy = { bitmap, question ->
                showCameraDialog = false
                viewModel.askSandyWithPhoto(bitmap, question)
            }
        )
    }

    // Sandy Notes Dialog
    if (showNotesDialog) {
        SandyNotesDialog(
            notes = notes,
            onAddNote = { viewModel.addNote(it) },
            onDeleteNote = { viewModel.deleteNote(it) },
            onDismiss = { showNotesDialog = false }
        )
    }
}

/**
 * Sandy Empty View with glowing 4-pointed Gemini-style star and action shortcut chips.
 */
@Composable
private fun SandyHeroEmptyView(
    activeModelName: String,
    quantization: String,
    onVoiceClick: () -> Unit,
    onCameraClick: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing 4-Pointed Gradient Star from Screenshot
        GeminiSparkleStar(size = 54.dp)

        Spacer(modifier = Modifier.height(20.dp))

        // Headline
        Text(
            text = "Your move, Human!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "I am Sandy, your on-device AI assistant ($activeModelName • $quantization)",
            fontSize = 13.sp,
            color = NeonCyan,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Pills: Chat / Hands-Free Voice
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                    .clickable(onClick = onVoiceClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hands-Free Voice", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                    .clickable(onClick = onCameraClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera Q&A", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Sandy Quick Phone Actions
        val actionPrompts = listOf(
            Triple(Icons.Default.Phone, "Sandy, call 9876543210", "Phone Call"),
            Triple(Icons.Default.Alarm, "Sandy, set alarm for 7:30 AM", "Alarm Clock"),
            Triple(Icons.Default.EditNote, "Sandy, note this down: Buy groceries and bread", "Voice Note"),
            Triple(Icons.Default.PlayCircle, "Sandy, play lofi beats on YouTube", "YouTube Music")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((icon, prompt, label) in actionPrompts) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161F30).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF26334D), RoundedCornerShape(14.dp))
                        .clickable { onQuickPrompt(prompt) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Text(text = prompt, fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom Input Pill Container with:
 * - Real-time Audio Waveform when user is speaking
 * - "+" Button to import .gguf files
 * - Camera Button for visual question answering
 * - Text input field
 * - Mic button
 * - Live Voice button (blue circular button with speech/waveform icon)
 */
@Composable
private fun ChatInputPillContainer(
    text: String,
    isGenerating: Boolean,
    voiceState: VoiceState,
    audioLevel: Float,
    partialTranscript: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onStartSpeech: () -> Unit,
    onCancelSpeech: () -> Unit,
    onSendSpeech: () -> Unit,
    onCameraClick: () -> Unit,
    onPickGgufFile: () -> Unit,
    onOpenLiveVoice: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (voiceState == VoiceState.LISTENING) {
            // Real-Time Audio Waveform Component actively listening to user's voice
            RealtimeSpeechRecordingBar(
                audioLevel = audioLevel,
                transcript = partialTranscript,
                onCancel = onCancelSpeech,
                onSend = onSendSpeech
            )
        } else {
            // Gemini-style Pill Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1A1F2C))
                    .border(1.dp, Color(0xFF2A3447), RoundedCornerShape(32.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "+" Button to import .gguf file from device
                IconButton(
                    onClick = onPickGgufFile,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("import_gguf_pill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import GGUF model file",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Camera Button for visual Q&A
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("camera_pill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take picture for Sandy to analyze",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            text = "Call Sandy or ask anything...",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    maxLines = 3
                )

                // Mic Button (activates real-time audio waveform component)
                IconButton(
                    onClick = onStartSpeech,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mic_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak to Sandy",
                        tint = if (text.isEmpty()) Color(0xFF94A3B8) else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dedicated Live Voice / Waveform Button (Gemini-style circular blue button)
                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .testTag("stop_generation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Generation",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (text.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send to Sandy",
                            tint = Color(0xFF003549),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Gemini Live Voice Button
                    IconButton(
                        onClick = onOpenLiveVoice,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .testTag("live_voice_gemini_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Sandy Live Voice Mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonaChip(
    persona: PersonaPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val border = if (isSelected) NeonCyan else Color.Transparent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = persona.icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = persona.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    capturedPhoto: Bitmap? = null,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // If message has photo attached, show image preview
        if (capturedPhoto != null) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    .padding(bottom = 6.dp)
            ) {
                Image(
                    bitmap = capturedPhoto.asImageBitmap(),
                    contentDescription = "Attached Camera Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(14.dp)
        ) {
            MarkdownText(
                text = message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        // Assistant Message Stats & Action Bar
        if (!isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.tokensPerSecond > 0) {
                    Text(
                        text = "⚡ ${String.format("%.1f", message.tokensPerSecond)} t/s • ${message.generationTimeMs}ms",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }

                IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read aloud",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    content: String,
    stats: InferenceStats,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(14.dp)
        ) {
            Column {
                MarkdownText(
                    text = if (content.isEmpty()) "Sandy is thinking..." else content,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ ${stats.formattedTps} tokens/sec",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            .clickable(onClick = onStop)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stop",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

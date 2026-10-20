package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.components.SandyFilesDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AudioWaveformBars
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.RealtimeAudioWaveform
import com.example.ui.components.SandyHistoryDialog
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioLevel by viewModel.voiceManager.audioLevel.collectAsState()
    val partialTranscript by viewModel.voiceManager.partialTranscript.collectAsState()
    val continuousVoice by viewModel.continuousVoiceMode.collectAsState()
    val activeModel by viewModel.modelRepository.activeModel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val streamingMessage by viewModel.streamingMessage.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()
    val interactionHistory by viewModel.interactionHistory.collectAsState()
    val uploadedFiles by viewModel.uploadedFiles.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFilesDialog by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.voiceManager.startListening()
        }
    }

    // Handle back button to return to chat screen
    BackHandler {
        viewModel.voiceManager.stopSpeaking()
        viewModel.voiceManager.stopListening()
        viewModel.navigateTo(AppScreen.CHAT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MintEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sandy Voice (${activeModel?.name ?: "Local"})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.voiceManager.stopSpeaking()
                            viewModel.voiceManager.stopListening()
                            viewModel.navigateTo(AppScreen.CHAT)
                        },
                        modifier = Modifier.testTag("exit_voice_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Uploaded Files Button
                    IconButton(
                        onClick = { showFilesDialog = true },
                        modifier = Modifier.testTag("voice_screen_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Uploaded Files & Documents",
                            tint = if (uploadedFiles.isNotEmpty()) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interaction History Button
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("voice_screen_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Interaction History",
                            tint = NeonCyan
                        )
                    }

                    // Continuous Voice Mode Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Hands-free",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = continuousVoice,
                            onCheckedChange = { viewModel.toggleContinuousVoice() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("handsfree_voice_switch")
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Status Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                val statusText = when (voiceState) {
                    VoiceState.IDLE -> "Tap microphone to speak"
                    VoiceState.LISTENING -> "Listening to your voice..."
                    VoiceState.PROCESSING -> "Inferencing on device (${currentStats.formattedTps} t/s)..."
                    VoiceState.SPEAKING -> "Speaking aloud..."
                    VoiceState.ERROR -> "Voice service unavailable. Tap to retry."
                }

                val statusColor = when (voiceState) {
                    VoiceState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    VoiceState.LISTENING -> MintEmerald
                    VoiceState.PROCESSING -> NeonPurple
                    VoiceState.SPEAKING -> ElectricViolet
                    VoiceState.ERROR -> MaterialTheme.colorScheme.error
                }

                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "100% Offline • ${activeModel?.quantization ?: "Q4_K_M"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Central Pulsing Orb & Waveform Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                AudioWaveformVisualizer(
                    voiceState = voiceState,
                    audioLevel = audioLevel,
                    size = 200.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time sinusoidal fluid waveform ribbon
                RealtimeAudioWaveform(
                    audioLevel = audioLevel,
                    isSpeaking = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Transcript or Latest Assistant Reply Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (partialTranscript.isNotBlank() && voiceState == VoiceState.LISTENING) {
                        Text(
                            text = "YOU (Speaking):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = partialTranscript,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (streamingMessage != null) {
                        Text(
                            text = "AI (Generating):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = streamingMessage.orEmpty(),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val lastAssistant = messages.lastOrNull { it.role == "assistant" }
                        if (lastAssistant != null) {
                            Text(
                                text = "AI (Response):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricViolet
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastAssistant.content,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4
                            )
                        } else {
                            Text(
                                text = "Say 'Sandy, call 9876543210', 'Sandy, set alarm for 7 AM', 'Sandy, note this down...', or ask any question.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch to Text
                IconButton(
                    onClick = {
                        viewModel.voiceManager.stopSpeaking()
                        viewModel.voiceManager.stopListening()
                        viewModel.navigateTo(AppScreen.CHAT)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("switch_to_text_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Switch to Text",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Main Voice Mic Button (72dp)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            when (voiceState) {
                                VoiceState.LISTENING -> MintEmerald
                                VoiceState.SPEAKING -> ElectricViolet
                                else -> NeonCyan
                            }
                        )
                        .clickable {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (voiceState == VoiceState.LISTENING) {
                                    viewModel.voiceManager.stopListening()
                                } else if (voiceState == VoiceState.SPEAKING) {
                                    viewModel.voiceManager.stopSpeaking()
                                } else {
                                    viewModel.voiceManager.startListening()
                                }
                            }
                        }
                        .testTag("main_voice_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Toggle Speech Recognition",
                        tint = Color(0xFF003549),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Stop / Silence button
                IconButton(
                    onClick = {
                        viewModel.voiceManager.stopSpeaking()
                        viewModel.voiceManager.stopListening()
                        viewModel.stopGeneration()
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("stop_voice_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Mute / Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Sandy Interaction History Dialog in Voice Mode
    if (showHistoryDialog) {
        SandyHistoryDialog(
            interactions = interactionHistory,
            onRerun = { interaction ->
                showHistoryDialog = false
                viewModel.sendUserPrompt(interaction.command, isVoiceInput = true)
            },
            onDelete = { id -> viewModel.deleteInteraction(id) },
            onClearAll = { viewModel.clearAllInteractions() },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Sandy Uploaded Files Dialog in Voice Mode
    if (showFilesDialog) {
        SandyFilesDialog(
            files = uploadedFiles,
            onAskAboutFile = { file ->
                showFilesDialog = false
                viewModel.sendUserPrompt("Tell me about the file ${file.fileName}", isVoiceInput = true)
            },
            onDeleteFile = { id -> viewModel.deleteUploadedFile(id) },
            onClearAll = { viewModel.clearAllUploadedFiles() },
            onPickPdf = { /* File picking handled in Chat screen */ },
            onPickJpg = { /* File picking handled in Chat screen */ },
            onLoadSamplePdf = { viewModel.uploadSampleFile("PDF_INVOICE") },
            onLoadSampleJpg = { viewModel.uploadSampleFile("JPG_RECEIPT") },
            onDismiss = { showFilesDialog = false }
        )
    }
}

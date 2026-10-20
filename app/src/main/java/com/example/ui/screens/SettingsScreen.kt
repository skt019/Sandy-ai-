package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.SandyFilesDialog
import com.example.ui.components.SandyHistoryDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InferenceParams
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val params by viewModel.params.collectAsState()
    val autoSpeak by viewModel.voiceManager.autoSpeak.collectAsState()
    val speechRate by viewModel.voiceManager.speechRate.collectAsState()
    val pitch by viewModel.voiceManager.pitch.collectAsState()
    val interactionHistory by viewModel.interactionHistory.collectAsState()
    val uploadedFiles by viewModel.uploadedFiles.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showFilesDialog by remember { mutableStateOf(false) }
    var showDeveloperName by remember { mutableStateOf(true) }

    var temp by remember(params.temperature) { mutableFloatStateOf(params.temperature) }
    var topP by remember(params.topP) { mutableFloatStateOf(params.topP) }
    var threads by remember(params.cpuThreads) { mutableIntStateOf(params.cpuThreads) }
    var maxTokens by remember(params.maxTokens) { mutableIntStateOf(params.maxTokens) }
    var contextLen by remember(params.contextLength) { mutableIntStateOf(params.contextLength) }
    var systemPrompt by remember(params.systemPrompt) { mutableStateOf(params.systemPrompt) }

    val scrollState = rememberScrollState()

    BackHandler {
        viewModel.navigateTo(AppScreen.CHAT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Model Tuning", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHAT) },
                        modifier = Modifier.testTag("back_to_chat_from_settings")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            temp = 0.7f
                            topP = 0.9f
                            threads = 4
                            maxTokens = 512
                            contextLen = 2048
                            systemPrompt = "You are a helpful, smart, and friendly AI assistant running locally on this Android device."
                            viewModel.updateParams(
                                InferenceParams(
                                    temperature = 0.7f,
                                    topP = 0.9f,
                                    cpuThreads = 4,
                                    maxTokens = 512,
                                    contextLength = 2048,
                                    systemPrompt = systemPrompt
                                )
                            )
                            Toast.makeText(context, "Reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset Defaults")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Local Inference Engine Parameters
            SettingsCard(
                title = "Inference Hyperparameters",
                icon = Icons.Default.Tune,
                accentColor = NeonCyan
            ) {
                // Temperature Slider
                SliderSettingItem(
                    label = "Temperature",
                    valueText = String.format("%.2f", temp),
                    description = "Higher values increase creativity; lower values make output more deterministic.",
                    value = temp,
                    onValueChange = {
                        temp = it
                        viewModel.updateParams(params.copy(temperature = it))
                    },
                    valueRange = 0.1f..1.5f,
                    steps = 13
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Top-P Slider
                SliderSettingItem(
                    label = "Top-P (Nucleus Sampling)",
                    valueText = String.format("%.2f", topP),
                    description = "Limits token candidate pool by cumulative probability threshold.",
                    value = topP,
                    onValueChange = {
                        topP = it
                        viewModel.updateParams(params.copy(topP = it))
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CPU Threads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CPU Worker Threads", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Number of cores allocated to matrix ops", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "$threads Cores",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = threads.toFloat(),
                    onValueChange = {
                        threads = it.toInt()
                        viewModel.updateParams(params.copy(cpuThreads = it.toInt()))
                    },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Max Tokens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Max Output Tokens", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Maximum tokens per response generation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "$maxTokens",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = {
                        maxTokens = it.toInt()
                        viewModel.updateParams(params.copy(maxTokens = it.toInt()))
                    },
                    valueRange = 128f..2048f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // Section 2: Voice Chat Settings
            SettingsCard(
                title = "Voice Chat Engine",
                icon = Icons.Default.Mic,
                accentColor = ElectricViolet
            ) {
                // Auto-speak toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Speak Responses", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Automatically read assistant answers aloud using TTS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.voiceManager.setAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricViolet,
                            checkedTrackColor = ElectricViolet.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speech Rate
                SliderSettingItem(
                    label = "Speech Speed",
                    valueText = "${String.format("%.2f", speechRate)}x",
                    description = "TTS vocal delivery speed",
                    value = speechRate,
                    onValueChange = { viewModel.voiceManager.setSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Pitch
                SliderSettingItem(
                    label = "Voice Pitch",
                    valueText = "${String.format("%.2f", pitch)}x",
                    description = "TTS frequency tone modulation",
                    value = pitch,
                    onValueChange = { viewModel.voiceManager.setPitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }

            // Section 3: System Prompt Customization
            SettingsCard(
                title = "System Persona Prompt",
                icon = Icons.Default.Psychology,
                accentColor = NeonPurple
            ) {
                Text(
                    text = "Controls the behavior, tone, and system instruction of loaded models.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = {
                        systemPrompt = it
                        viewModel.updateParams(params.copy(systemPrompt = it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    maxLines = 6
                )
            }

            // Section 4: Engine Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MintEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline Privacy Guarantee",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All voice synthesis, speech recognition, and GGUF neural network processing run 100% on this smartphone's processor. No external servers or API keys are required.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section 4: Interaction History & Room Database Logs
            SettingsCard(
                title = "Voice Commands & Action History",
                icon = Icons.Default.History,
                accentColor = NeonCyan
            ) {
                Text(
                    text = "Sandy logs voice commands, phone actions (calls, WhatsApp, alarms, notes, camera), and responses locally in a Room database so you can review and replay past interactions.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Logged Actions: ${interactionHistory.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        modifier = Modifier.testTag("review_history_from_settings_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Review Logs", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 5: Uploaded Files & Room Database Documents (PDF / JPG)
            SettingsCard(
                title = "Uploaded Documents & Files (PDF / JPG)",
                icon = Icons.Default.Description,
                accentColor = NeonCyan
            ) {
                Text(
                    text = "Sandy processes uploaded PDF documents and JPG images, extracting metadata, page counts, structured text, and executive summaries stored locally in Room.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stored Documents: ${uploadedFiles.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { showFilesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        modifier = Modifier.testTag("manage_files_from_settings_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Files", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 6: About Sandy & Developer Information
            SettingsCard(
                title = "About & Developer Information",
                icon = Icons.Default.Info,
                accentColor = MintEmerald
            ) {
                // Developer Name Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Developer Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Enable to view creator credits",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = showDeveloperName,
                        onCheckedChange = { showDeveloperName = it },
                        modifier = Modifier.testTag("settings_developer_name_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonCyan,
                            uncheckedTrackColor = Color(0xFF26334D)
                        )
                    )
                }

                if (showDeveloperName) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131A29))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Developers:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Santosh Thakur",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Nasir Raza",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contact Us:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "skt019@gmail.com",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Sandy Interaction History Dialog
    if (showHistoryDialog) {
        SandyHistoryDialog(
            interactions = interactionHistory,
            onRerun = { interaction ->
                showHistoryDialog = false
                viewModel.sendUserPrompt(interaction.command, isVoiceInput = interaction.isVoice)
                viewModel.navigateTo(AppScreen.CHAT)
            },
            onDelete = { id -> viewModel.deleteInteraction(id) },
            onClearAll = { viewModel.clearAllInteractions() },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Sandy Uploaded Files Dialog
    if (showFilesDialog) {
        SandyFilesDialog(
            files = uploadedFiles,
            onAskAboutFile = { file ->
                showFilesDialog = false
                viewModel.askAboutUploadedFile(file)
                viewModel.navigateTo(AppScreen.CHAT)
            },
            onDeleteFile = { id -> viewModel.deleteUploadedFile(id) },
            onClearAll = { viewModel.clearAllUploadedFiles() },
            onPickPdf = { /* Picking handled in chat */ },
            onPickJpg = { /* Picking handled in chat */ },
            onLoadSamplePdf = { viewModel.uploadSampleFile("PDF_INVOICE") },
            onLoadSampleJpg = { viewModel.uploadSampleFile("JPG_RECEIPT") },
            onDismiss = { showFilesDialog = false }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun SliderSettingItem(
    label: String,
    valueText: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
        )
    }
}

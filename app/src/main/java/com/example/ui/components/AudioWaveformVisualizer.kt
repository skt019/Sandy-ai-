package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.voice.VoiceState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time continuous fluid multi-wave audio visualizer.
 * Renders multiple harmonic sinusoidal bezier ribbons that undulate dynamically
 * according to the live microphone amplitude.
 */
@Composable
fun RealtimeAudioWaveform(
    audioLevel: Float, // 0.0f to 1.0f from SpeechRecognizer RMS
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    waveColors: List<Color> = listOf(NeonCyan, ElectricViolet, MintEmerald)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Boost amplitude when actively speaking or picking up voice
        val effectiveAmp = if (isSpeaking) {
            (audioLevel * 0.85f + 0.15f).coerceIn(0.12f, 1f) * (height * 0.42f)
        } else {
            (height * 0.08f)
        }

        // Draw multiple harmonic glowing wave ribbons
        val waves = listOf(
            Triple(1.0f, 1.0f, waveColors[0]),
            Triple(1.5f, -0.8f, waveColors[1 % waveColors.size]),
            Triple(2.0f, 0.6f, waveColors[2 % waveColors.size])
        )

        for ((freq, ampScale, color) in waves) {
            val path = Path()
            val step = 4f
            var first = true

            for (x in 0..width.toInt() step step.toInt()) {
                val progress = x / width
                // Envelope window: dampen at edges so wave stays within bounds
                val envelope = sin(progress * PI.toFloat())
                val currentPhase = phase * freq
                val y = midY + sin(progress * 2f * PI.toFloat() * freq + currentPhase) * effectiveAmp * ampScale * envelope

                if (first) {
                    path.moveTo(x.toFloat(), y)
                    first = false
                } else {
                    path.lineTo(x.toFloat(), y)
                }
            }

            // Draw wave line with glow
            drawPath(
                path = path,
                color = color.copy(alpha = if (isSpeaking) 0.85f else 0.45f),
                style = Stroke(
                    width = if (isSpeaking) 3.5.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Interactive Real-Time Speech Recording Bar
 * Shows real-time waveform while user is speaking into the chat,
 * displaying live transcription and quick Send / Cancel actions.
 */
@Composable
fun RealtimeSpeechRecordingBar(
    audioLevel: Float,
    transcript: String,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, NeonCyan, RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("realtime_speech_recording_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel Button
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    .testTag("cancel_speech_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Voice Input",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Central Waveform + Live Transcript
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real-time Waveform Canvas
                RealtimeAudioWaveform(
                    audioLevel = audioLevel,
                    isSpeaking = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )

                Text(
                    text = if (transcript.isNotBlank()) transcript else "Listening... speak now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (transcript.isNotBlank()) MaterialTheme.colorScheme.onSurface else NeonCyan,
                    maxLines = 1
                )
            }

            // Send Button
            IconButton(
                onClick = onSend,
                enabled = transcript.isNotBlank(),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (transcript.isNotBlank()) NeonCyan else MaterialTheme.colorScheme.surface)
                    .testTag("send_speech_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Transcribed Prompt",
                    tint = if (transcript.isNotBlank()) Color(0xFF003549) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Four-pointed glowing Gemini AI Sparkle Star as featured in the reference screenshot.
 */
@Composable
fun GeminiSparkleStar(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    Box(
        modifier = modifier.size(size * pulse),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Four-pointed star path with concave bezier curves
            val path = Path().apply {
                moveTo(cx, 0f)
                // Curve to right point
                quadraticTo(cx * 1.15f, cy * 0.85f, w, cy)
                // Curve to bottom point
                quadraticTo(cx * 1.15f, cy * 1.15f, cx, h)
                // Curve to left point
                quadraticTo(cx * 0.85f, cy * 1.15f, 0f, cy)
                // Curve back to top point
                quadraticTo(cx * 0.85f, cy * 0.85f, cx, 0f)
                close()
            }

            // Vibrant multi-color gradient (Coral, Amber, Neon Cyan, Violet)
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF97316), // Coral Orange
                    Color(0xFFFBBF24), // Golden Amber
                    Color(0xFF38BDF8), // Electric Cyan
                    Color(0xFF818CF8)  // Violet
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            // Outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.75f
                ),
                radius = w * 0.75f,
                center = Offset(cx, cy)
            )

            drawPath(path = path, brush = brush)
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    voiceState: VoiceState,
    audioLevel: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_anim")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor = when (voiceState) {
        VoiceState.IDLE -> NeonCyan
        VoiceState.LISTENING -> MintEmerald
        VoiceState.PROCESSING -> NeonPurple
        VoiceState.SPEAKING -> ElectricViolet
        VoiceState.ERROR -> MaterialTheme.colorScheme.error
    }

    val secondaryColor = when (voiceState) {
        VoiceState.IDLE -> ElectricViolet
        VoiceState.LISTENING -> NeonCyan
        VoiceState.PROCESSING -> MintEmerald
        VoiceState.SPEAKING -> NeonPurple
        VoiceState.ERROR -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val baseRadius = (canvasWidth / 4.2f)

            // Scaled radius reacting to real microphone level
            val activeScale = 1f + (audioLevel * 0.45f)
            val radius = baseRadius * activeScale * (if (voiceState == VoiceState.IDLE) pulseAnim else 1.0f)

            // Outer ambient glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.8f
                ),
                radius = radius * 1.8f,
                center = center
            )

            // Outer reactive ripples
            val rippleCount = 3
            for (i in 1..rippleCount) {
                val rippleRadius = radius + (i * 18.dp.toPx() * (audioLevel + 0.3f))
                val alpha = (0.4f / i) * (audioLevel.coerceAtLeast(0.2f))
                drawCircle(
                    color = coreColor.copy(alpha = alpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Central Glowing Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor,
                        secondaryColor,
                        Color(0xFF0F172A)
                    ),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Rotating Cyber Orbit Ring when processing
            if (voiceState == VoiceState.PROCESSING) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.7f),
                    radius = radius * 1.25f,
                    center = center,
                    style = Stroke(
                        width = 3.dp.toPx()
                    )
                )
            }
        }
    }
}

@Composable
fun AudioWaveformBars(
    voiceState: VoiceState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 16
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bars_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val activeColor = when (voiceState) {
        VoiceState.IDLE -> NeonCyan
        VoiceState.LISTENING -> MintEmerald
        VoiceState.PROCESSING -> NeonPurple
        VoiceState.SPEAKING -> ElectricViolet
        VoiceState.ERROR -> MaterialTheme.colorScheme.error
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = totalWidth / (barCount * 1.6f)
        val gap = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val phase = (i.toFloat() / barCount) * 3.14f * 2f + waveOffset
            val wave = ((sin(phase) + 1f) / 2f)
            val barAmp = when (voiceState) {
                VoiceState.LISTENING, VoiceState.SPEAKING -> {
                    (wave * 0.5f + audioLevel * 0.5f).coerceIn(0.15f, 1f)
                }
                VoiceState.PROCESSING -> {
                    (wave * 0.7f + 0.3f).coerceIn(0.2f, 0.9f)
                }
                else -> {
                    0.15f
                }
            }

            val barHeight = maxHeight * barAmp
            val x = i * (barWidth + gap)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = activeColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

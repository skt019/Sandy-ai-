package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun CameraAssistantDialog(
    capturedBitmap: Bitmap?,
    onRetake: () -> Unit,
    onDismiss: () -> Unit,
    onAskSandy: (Bitmap, String) -> Unit
) {
    var questionText by remember { mutableStateOf("Describe what Sandy sees in this photo and identify key details.") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("camera_assistant_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sandy Visual Q&A",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                // Image preview or placeholder
                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Captured Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Retake button overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable(onClick = onRetake)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Retake Photo", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(onClick = onRetake),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tap to capture photo with Camera", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                        }
                    }
                }

                // Quick question suggestions
                val suggestions = listOf(
                    "What is this object?",
                    "Read & transcribe text",
                    "Analyze and solve math/code"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (s in suggestions) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { questionText = s }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = s, fontSize = 10.sp, color = NeonCyan, maxLines = 1)
                        }
                    }
                }

                // Prompt Input
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    placeholder = { Text("Ask Sandy about this picture...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    maxLines = 3
                )

                // Ask Button
                Button(
                    onClick = {
                        if (capturedBitmap != null && questionText.isNotBlank()) {
                            onAskSandy(capturedBitmap, questionText)
                        } else {
                            onRetake()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_sandy_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (capturedBitmap != null) "Ask Sandy to Respond" else "Capture Photo First",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

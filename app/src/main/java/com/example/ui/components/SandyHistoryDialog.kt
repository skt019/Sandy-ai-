package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SandyInteractionEntity
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SandyHistoryDialog(
    interactions: List<SandyInteractionEntity>,
    onRerun: (SandyInteractionEntity) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filterOptions = listOf(
        Pair("ALL", "All (${interactions.size})"),
        Pair("VOICE", "🎙️ Voice"),
        Pair("ACTIONS", "⚡ Phone Actions"),
        Pair("WHATSAPP", "💬 WhatsApp"),
        Pair("CALL_SMS", "📞 Call & SMS"),
        Pair("NOTES", "📝 Notes"),
        Pair("CAMERA", "📷 Camera")
    )

    val filteredList = interactions.filter { item ->
        when (selectedFilter) {
            "ALL" -> true
            "VOICE" -> item.isVoice
            "ACTIONS" -> item.actionType != "CONVERSATION"
            "WHATSAPP" -> item.actionType == "WHATSAPP"
            "CALL_SMS" -> item.actionType == "CALL" || item.actionType == "SMS"
            "NOTES" -> item.actionType == "NOTE"
            "CAMERA" -> item.actionType == "CAMERA_QA"
            else -> true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("sandy_history_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Interaction History", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Logged locally in Room database", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Row {
                        if (interactions.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAll,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_history_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color(0xFFEF4444))
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filterOptions) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // History List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (interactions.isEmpty()) {
                                "No interactions logged yet.\nSpeak or type any command to Sandy!"
                            } else {
                                "No logs match this filter."
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            HistoryItemCard(
                                item = item,
                                onRerun = { onRerun(item) },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("history", item.command))
                                    Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { onDelete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: SandyInteractionEntity,
    onRerun: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    val (icon: ImageVector, accentColor: Color) = when (item.actionType) {
        "CALL" -> Pair(Icons.Default.Phone, NeonCyan)
        "WHATSAPP" -> Pair(Icons.Default.Chat, MintEmerald)
        "SMS" -> Pair(Icons.Default.Sms, Color(0xFF38BDF8))
        "YOUTUBE" -> Pair(Icons.Default.PlayCircle, Color(0xFFEF4444))
        "ALARM", "TIMER" -> Pair(Icons.Default.Alarm, Color(0xFFF59E0B))
        "NOTE" -> Pair(Icons.Default.EditNote, NeonPurple)
        "CAMERA_QA" -> Pair(Icons.Default.CameraAlt, NeonCyan)
        else -> Pair(Icons.Default.Psychology, ElectricViolet)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("history_item_${item.id}")
    ) {
        Column {
            // Top Row: Type Badge + Input Mode + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.actionType.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Input mode badge (Voice vs Text)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.isVoice) Icons.Default.Mic else Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = if (item.isVoice) MintEmerald else Color(0xFF94A3B8),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (item.isVoice) "Voice" else "Text",
                                fontSize = 9.sp,
                                color = if (item.isVoice) MintEmerald else Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(text = dateStr, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Command Text
            Text(
                text = item.command,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 18.sp
            )

            // Sandy Summary
            if (item.actionSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sandy: ${item.actionSummary}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions: Rerun, Copy, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Re-run Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .clickable(onClick = onRerun)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Again", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

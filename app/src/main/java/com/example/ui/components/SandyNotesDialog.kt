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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.NoteEntity
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SandyNotesDialog(
    notes: List<NoteEntity>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newNoteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("sandy_notes_dialog")
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sandy Notes (${notes.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick add note input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        placeholder = { Text("Note whatever you say...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                onAddNote(newNoteText.trim())
                                newNoteText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes List
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes recorded yet.\nSay 'Sandy, note this down: ...' anytime!",
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
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("note", note.content))
                                    Toast.makeText(context, "Note copied", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { onDeleteNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = dateStr, fontSize = 11.sp, color = NeonCyan)
                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.content, fontSize = 13.sp, color = Color.White, lineHeight = 18.sp)
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.actions.PhoneActionHandler
import com.example.actions.SandyAction
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun SandyActionCard(
    action: SandyAction,
    onExecuteOverride: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (icon: ImageVector, title: String, subtitle: String, buttonText: String, accentColor: Color) = when (action) {
        is SandyAction.WhatsApp -> {
            val contact = action.recipient ?: "WhatsApp Contact"
            Tuple5(
                Icons.Default.Chat,
                "WhatsApp Message",
                "To: $contact\n\"${action.message}\"",
                "Send on WhatsApp",
                MintEmerald
            )
        }
        is SandyAction.Call -> {
            Tuple5(
                Icons.Default.Phone,
                "Phone Call",
                "Call: ${action.contactName ?: action.phoneNumber}",
                "Open Phone Dialer",
                NeonCyan
            )
        }
        is SandyAction.Sms -> {
            Tuple5(
                Icons.Default.Sms,
                "Send SMS",
                "To: ${action.recipient}\n\"${action.message}\"",
                "Open SMS App",
                Color(0xFF38BDF8)
            )
        }
        is SandyAction.YouTube -> {
            Tuple5(
                Icons.Default.PlayCircle,
                "YouTube Video",
                "Search & Play: \"${action.query}\"",
                "Play on YouTube",
                Color(0xFFEF4444)
            )
        }
        is SandyAction.Alarm -> {
            val amPm = if (action.hour >= 12) "PM" else "AM"
            val displayHour = if (action.hour == 0) 12 else if (action.hour > 12) action.hour - 12 else action.hour
            val displayMinute = String.format("%02d", action.minute)
            Tuple5(
                Icons.Default.Alarm,
                "Set Alarm",
                "Alarm at $displayHour:$displayMinute $amPm",
                "Confirm Alarm",
                Color(0xFFF59E0B)
            )
        }
        is SandyAction.Timer -> {
            Tuple5(
                Icons.Default.HourglassBottom,
                "Set Timer",
                "Timer for ${action.seconds / 60} min (${action.seconds}s)",
                "Start Timer",
                Color(0xFFF59E0B)
            )
        }
        is SandyAction.Note -> {
            Tuple5(
                Icons.Default.EditNote,
                "Saved to Sandy Notes",
                "\"${action.content}\"",
                "Saved Locally",
                NeonPurple
            )
        }
        is SandyAction.CameraQuestion -> {
            Tuple5(
                Icons.Default.CameraAlt,
                "Camera Visual Analysis",
                action.question,
                "Open Camera",
                NeonCyan
            )
        }
        is SandyAction.FileQuery -> {
            Tuple5(
                Icons.Default.Description,
                "Uploaded File Information",
                action.query,
                "View Document Data",
                NeonCyan
            )
        }
        is SandyAction.WhatsAppStatus -> {
            Tuple5(
                Icons.Default.Chat,
                "WhatsApp Status Update",
                "Status: \"${action.statusText}\"",
                "Post to WhatsApp Status",
                MintEmerald
            )
        }
        is SandyAction.InstagramMessage -> {
            val user = action.recipient.trim().removePrefix("@")
            Tuple5(
                Icons.Default.Send,
                "Instagram Direct Message",
                "To: @$user\n\"${action.message}\"",
                "Open Instagram Chat",
                Color(0xFFE1306C)
            )
        }
        is SandyAction.InstagramPost -> {
            Tuple5(
                Icons.Default.Share,
                "Instagram Post",
                "Caption: \"${action.caption}\"",
                "Share on Instagram",
                Color(0xFFE1306C)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131B2B))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("sandy_action_card")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8), maxLines = 2)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (onExecuteOverride != null) {
                        onExecuteOverride()
                    } else {
                        PhoneActionHandler.executeAction(context, action)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_action_button")
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

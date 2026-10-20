package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sandy_interactions")
data class SandyInteractionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val command: String,
    val actionType: String, // "CALL", "WHATSAPP", "SMS", "YOUTUBE", "ALARM", "TIMER", "NOTE", "CAMERA_QA", "CONVERSATION"
    val actionSummary: String,
    val isVoice: Boolean = false,
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis()
)

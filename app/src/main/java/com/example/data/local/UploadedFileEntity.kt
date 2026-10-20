package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_files")
data class UploadedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileType: String, // "PDF" or "JPG"
    val fileUri: String,
    val fileSizeBytes: Long,
    val fileSizeFormatted: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1, // Number of pages for PDF, 1 for JPG
    val dimensions: String? = null, // e.g. "1920x1080 px" or "A4 (595x842 pt)"
    val extractedText: String = "",
    val summary: String = "",
    val keyInsights: String = "",
    val metadataJson: String = "", // Detailed JSON string with author, resolution, mimeType, word count, etc.
    val analysisStatus: String = "ANALYZED"
)

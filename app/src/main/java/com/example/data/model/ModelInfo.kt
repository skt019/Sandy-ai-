package com.example.data.model

enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    LOADED,
    ERROR
}

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val architecture: String,
    val quantization: String,
    val parameterCount: String,
    val sizeBytes: Long,
    val downloadUrl: String? = null,
    val localFilePath: String? = null,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadSpeedMb: Float = 0f,
    val downloadedBytes: Long = 0L,
    val isCustom: Boolean = false,
    val metadata: GgufMetadata? = null,
    val recommendedRamMb: Int = 1024,
    val defaultContextLength: Int = 2048
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.0f MB", mb)
            }
        }
}

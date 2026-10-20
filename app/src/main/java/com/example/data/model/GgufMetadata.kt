package com.example.data.model

data class GgufMetadata(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val magic: String, // "GGUF"
    val version: UInt,
    val tensorCount: ULong,
    val metadataKvCount: ULong,
    val architecture: String, // e.g., "llama", "qwen2", "smollm", "gemma"
    val modelName: String,
    val quantization: String, // e.g., "Q4_K_M", "Q8_0", "Q5_K_M", "F16"
    val contextLength: Int,
    val embeddingLength: Int,
    val blockCount: Int,
    val headCount: Int,
    val estimatedParams: String,
    val rawKvPairs: Map<String, String> = emptyMap(),
    val isValid: Boolean = true,
    val parseError: String? = null
) {
    val formattedFileSize: String
        get() {
            val mb = fileSize / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}

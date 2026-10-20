package com.example.data.model

data class InferenceParams(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val maxTokens: Int = 512,
    val contextLength: Int = 2048,
    val cpuThreads: Int = 4,
    val systemPrompt: String = "You are a helpful, smart, and friendly AI assistant running locally on this Android device."
)

data class InferenceStats(
    val tokensPerSecond: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val totalGenerationTimeMs: Long = 0L,
    val tokenCount: Int = 0,
    val memoryUsageMb: Long = 0L,
    val activeThreads: Int = 4,
    val isGenerating: Boolean = false
) {
    val formattedTps: String
        get() = String.format("%.1f", tokensPerSecond)
}

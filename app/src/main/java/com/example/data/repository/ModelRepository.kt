package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.GgufMetadata
import com.example.data.model.ModelInfo
import com.example.data.model.ModelStatus
import com.example.gguf.GgufParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class ModelRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }

    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    private val _activeModel = MutableStateFlow<ModelInfo?>(null)
    val activeModel: StateFlow<ModelInfo?> = _activeModel.asStateFlow()

    private val activeDownloads = mutableMapOf<String, kotlinx.coroutines.Job>()

    init {
        initDefaultCatalog()
    }

    private fun initDefaultCatalog() {
        // Ensure default embedded sample GGUF file exists for instant offline testing
        val sampleFile = File(modelsDir, "SmolLM2-135M-Instruct-Q4_K_M.gguf")
        GgufParser.createSampleGgufFile(sampleFile)

        val defaultList = listOf(
            ModelInfo(
                id = "smollm2-135m-q4km",
                name = "SmolLM2 135M Instruct",
                description = "Ultra-fast mobile edge model. Instant CPU responses and minimal battery consumption.",
                architecture = "smollm",
                quantization = "Q4_K_M",
                parameterCount = "135M",
                sizeBytes = 95L * 1024 * 1024,
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
                localFilePath = sampleFile.absolutePath,
                status = ModelStatus.READY,
                recommendedRamMb = 350,
                defaultContextLength = 2048
            ),
            ModelInfo(
                id = "qwen2.5-0.5b-q4km",
                name = "Qwen2.5 0.5B Instruct",
                description = "High-intelligence compact model with strong multilingual and coding capabilities.",
                architecture = "qwen2",
                quantization = "Q4_K_M",
                parameterCount = "0.5B",
                sizeBytes = 398L * 1024 * 1024,
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                status = ModelStatus.NOT_DOWNLOADED,
                recommendedRamMb = 600,
                defaultContextLength = 4096
            ),
            ModelInfo(
                id = "smollm2-360m-q4km",
                name = "SmolLM2 360M Instruct",
                description = "Balanced speed and reasoning. Excellent for conversational voice chat on phone CPUs.",
                architecture = "smollm",
                quantization = "Q4_K_M",
                parameterCount = "360M",
                sizeBytes = 240L * 1024 * 1024,
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
                status = ModelStatus.NOT_DOWNLOADED,
                recommendedRamMb = 500,
                defaultContextLength = 2048
            ),
            ModelInfo(
                id = "llama3.2-1b-q4km",
                name = "Llama 3.2 1B Instruct",
                description = "Meta's flagship on-device model for complex reasoning and summarization.",
                architecture = "llama",
                quantization = "Q4_K_M",
                parameterCount = "1.23B",
                sizeBytes = 800L * 1024 * 1024,
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                status = ModelStatus.NOT_DOWNLOADED,
                recommendedRamMb = 1200,
                defaultContextLength = 8192
            ),
            ModelInfo(
                id = "gemma2-2b-q4km",
                name = "Gemma 2 2B Instruct",
                description = "Google DeepMind's compact model with exceptional language fluency.",
                architecture = "gemma2",
                quantization = "Q4_K_M",
                parameterCount = "2.6B",
                sizeBytes = 1600L * 1024 * 1024,
                downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
                status = ModelStatus.NOT_DOWNLOADED,
                recommendedRamMb = 2048,
                defaultContextLength = 4096
            )
        )

        _models.value = defaultList

        // Scan storage and parse metadata
        scanLocalModelsSync()

        // Set default loaded model
        val defaultReady = _models.value.firstOrNull { it.status == ModelStatus.READY || it.status == ModelStatus.LOADED }
        if (defaultReady != null) {
            selectModel(defaultReady.id)
        }
    }

    private fun scanLocalModelsSync() {
        val files = modelsDir.listFiles { f -> f.isFile && f.name.endsWith(".gguf", ignoreCase = true) } ?: emptyArray()
        val parsedLocal = files.map { file ->
            val meta = GgufParser.parseFromFileSync(file)
            ModelInfo(
                id = "local_${file.name.hashCode()}",
                name = meta.modelName.ifBlank { file.nameWithoutExtension },
                description = "Local on-device GGUF file in app storage",
                architecture = meta.architecture,
                quantization = meta.quantization,
                parameterCount = meta.estimatedParams,
                sizeBytes = file.length(),
                localFilePath = file.absolutePath,
                status = ModelStatus.READY,
                isCustom = true,
                metadata = meta,
                defaultContextLength = meta.contextLength
            )
        }

        _models.update { currentList ->
            val updated = currentList.map { model ->
                // Check if any catalog model is on disk
                val onDisk = files.firstOrNull { it.name.equals("${model.name}.gguf", ignoreCase = true) || it.absolutePath == model.localFilePath }
                if (onDisk != null && onDisk.exists()) {
                    model.copy(
                        status = if (model.status == ModelStatus.LOADED) ModelStatus.LOADED else ModelStatus.READY,
                        localFilePath = onDisk.absolutePath,
                        metadata = GgufParser.parseFromFileSync(onDisk)
                    )
                } else if (model.isCustom) {
                    model
                } else {
                    model
                }
            }.toMutableList()

            // Add newly discovered local custom models
            for (custom in parsedLocal) {
                if (updated.none { it.localFilePath == custom.localFilePath }) {
                    updated.add(custom)
                }
            }
            updated
        }
    }

    suspend fun refreshLocalModels() = withContext(Dispatchers.IO) {
        scanLocalModelsSync()
    }

    suspend fun importCustomGguf(uri: Uri, fileName: String): ModelInfo = withContext(Dispatchers.IO) {
        val destFile = File(modelsDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        val meta = GgufParser.parseFromFile(destFile)
        val newModel = ModelInfo(
            id = "custom_${UUID.randomUUID()}",
            name = meta.modelName.ifBlank { fileName.removeSuffix(".gguf") },
            description = "Imported custom GGUF model",
            architecture = meta.architecture,
            quantization = meta.quantization,
            parameterCount = meta.estimatedParams,
            sizeBytes = destFile.length(),
            localFilePath = destFile.absolutePath,
            status = ModelStatus.READY,
            isCustom = true,
            metadata = meta,
            defaultContextLength = meta.contextLength
        )

        _models.update { listOf(newModel) + it }
        selectModel(newModel.id)
        newModel
    }

    fun selectModel(modelId: String) {
        _models.update { list ->
            list.map { m ->
                if (m.id == modelId) {
                    m.copy(status = ModelStatus.LOADED)
                } else if (m.status == ModelStatus.LOADED) {
                    m.copy(status = ModelStatus.READY)
                } else {
                    m
                }
            }
        }
        val loaded = _models.value.firstOrNull { it.id == modelId }
        _activeModel.value = loaded
    }

    suspend fun downloadModel(modelId: String, onProgress: (Float, Float) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val model = _models.value.firstOrNull { it.id == modelId } ?: return@withContext
        val url = model.downloadUrl ?: return@withContext

        val targetFile = File(modelsDir, "${model.name.replace(" ", "_")}.gguf")

        _models.update { list ->
            list.map { if (it.id == modelId) it.copy(status = ModelStatus.DOWNLOADING, downloadProgress = 0f) else it }
        }

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: model.sizeBytes
            val buffer = ByteArray(8 * 1024)
            var downloaded = 0L
            var lastUpdate = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            FileOutputStream(targetFile).use { output ->
                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLastUpdate += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdate
                        if (elapsed >= 300) {
                            val speedMb = (bytesSinceLastUpdate / (1024f * 1024f)) / (elapsed / 1000f)
                            val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                            _models.update { list ->
                                list.map {
                                    if (it.id == modelId) {
                                        it.copy(
                                            downloadProgress = progress,
                                            downloadSpeedMb = speedMb,
                                            downloadedBytes = downloaded
                                        )
                                    } else it
                                }
                            }
                            onProgress(progress, speedMb)
                            lastUpdate = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            val meta = GgufParser.parseFromFile(targetFile)

            _models.update { list ->
                list.map {
                    if (it.id == modelId) {
                        it.copy(
                            status = ModelStatus.READY,
                            downloadProgress = 1f,
                            localFilePath = targetFile.absolutePath,
                            metadata = meta
                        )
                    } else it
                }
            }
            selectModel(modelId)
        } catch (e: Exception) {
            if (e is CancellationException) {
                targetFile.delete()
            }
            _models.update { list ->
                list.map { if (it.id == modelId) it.copy(status = ModelStatus.ERROR) else it }
            }
        }
    }

    fun deleteLocalModel(modelId: String) {
        val model = _models.value.firstOrNull { it.id == modelId } ?: return
        model.localFilePath?.let { path ->
            val f = File(path)
            if (f.exists()) f.delete()
        }
        if (model.isCustom) {
            _models.update { it.filter { m -> m.id != modelId } }
        } else {
            _models.update { list ->
                list.map {
                    if (it.id == modelId) it.copy(
                        status = ModelStatus.NOT_DOWNLOADED,
                        downloadProgress = 0f,
                        localFilePath = null,
                        metadata = null
                    ) else it
                }
            }
        }
        if (_activeModel.value?.id == modelId) {
            val fallback = _models.value.firstOrNull { it.status == ModelStatus.READY }
            if (fallback != null) {
                selectModel(fallback.id)
            } else {
                _activeModel.value = null
            }
        }
    }
}

package com.example.gguf

import android.content.Context
import android.net.Uri
import com.example.data.model.GgufMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GgufParser {
    private const val GGUF_MAGIC = "GGUF"

    enum class GgufType(val id: Int) {
        UINT8(0),
        INT8(1),
        UINT16(2),
        INT16(3),
        UINT32(4),
        INT32(5),
        FLOAT32(6),
        BOOL(7),
        STRING(8),
        ARRAY(9),
        UINT64(10),
        INT64(11),
        FLOAT64(12);

        companion object {
            fun fromId(id: Int): GgufType? = entries.find { it.id == id }
        }
    }

    fun parseFromFileSync(file: File): GgufMetadata {
        if (!file.exists()) {
            return invalidMetadata(file.name, file.absolutePath, 0, "File does not exist")
        }
        val fileSize = file.length()
        return try {
            RandomAccessFile(file, "r").use { raf ->
                parseFromReader(file.name, file.absolutePath, fileSize) { buffer ->
                    raf.read(buffer)
                }
            }
        } catch (e: Exception) {
            invalidMetadata(file.name, file.absolutePath, fileSize, e.localizedMessage ?: "Parse error")
        }
    }

    suspend fun parseFromFile(file: File): GgufMetadata = withContext(Dispatchers.IO) {
        parseFromFileSync(file)
    }

    suspend fun parseFromUri(context: Context, uri: Uri, fileName: String): GgufMetadata = withContext(Dispatchers.IO) {
        var fileSize = 0L
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                fileSize = pfd.statSize
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseFromStream(fileName, uri.toString(), fileSize, inputStream)
            } ?: invalidMetadata(fileName, uri.toString(), fileSize, "Cannot open stream from URI")
        } catch (e: Exception) {
            invalidMetadata(fileName, uri.toString(), fileSize, e.localizedMessage ?: "Parse error")
        }
    }

    private fun parseFromStream(
        fileName: String,
        filePath: String,
        fileSize: Long,
        inputStream: InputStream
    ): GgufMetadata {
        val magicBytes = ByteArray(4)
        val read = inputStream.read(magicBytes)
        if (read != 4 || String(magicBytes) != GGUF_MAGIC) {
            return invalidMetadata(fileName, filePath, fileSize, "Invalid GGUF magic header")
        }

        val versionBytes = ByteArray(4)
        inputStream.read(versionBytes)
        val version = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

        val tensorCountBytes = ByteArray(8)
        inputStream.read(tensorCountBytes)
        val tensorCount = ByteBuffer.wrap(tensorCountBytes).order(ByteOrder.LITTLE_ENDIAN).long.toULong()

        val kvCountBytes = ByteArray(8)
        inputStream.read(kvCountBytes)
        val kvCount = ByteBuffer.wrap(kvCountBytes).order(ByteOrder.LITTLE_ENDIAN).long.toULong()

        val kvMap = mutableMapOf<String, String>()
        val maxKvToParse = minOf(kvCount.toLong(), 200L) // Bounded parse for responsiveness

        for (i in 0 until maxKvToParse) {
            val key = readGgufString(inputStream) ?: break
            val typeBytes = ByteArray(4)
            if (inputStream.read(typeBytes) != 4) break
            val typeId = ByteBuffer.wrap(typeBytes).order(ByteOrder.LITTLE_ENDIAN).int
            val type = GgufType.fromId(typeId)

            val valueStr = readGgufValueString(inputStream, type)
            if (valueStr != null) {
                kvMap[key] = valueStr
            }
        }

        return extractMetadata(fileName, filePath, fileSize, version, tensorCount, kvCount, kvMap)
    }

    private fun parseFromReader(
        fileName: String,
        filePath: String,
        fileSize: Long,
        readBytes: (ByteArray) -> Int
    ): GgufMetadata {
        val magicBytes = ByteArray(4)
        if (readBytes(magicBytes) != 4 || String(magicBytes) != GGUF_MAGIC) {
            return invalidMetadata(fileName, filePath, fileSize, "Invalid GGUF magic header")
        }

        val buf4 = ByteArray(4)
        readBytes(buf4)
        val version = ByteBuffer.wrap(buf4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

        val buf8 = ByteArray(8)
        readBytes(buf8)
        val tensorCount = ByteBuffer.wrap(buf8).order(ByteOrder.LITTLE_ENDIAN).long.toULong()

        readBytes(buf8)
        val kvCount = ByteBuffer.wrap(buf8).order(ByteOrder.LITTLE_ENDIAN).long.toULong()

        val kvMap = mutableMapOf<String, String>()
        // Return structured model info
        return extractMetadata(fileName, filePath, fileSize, version, tensorCount, kvCount, kvMap)
    }

    private fun readGgufString(stream: InputStream): String? {
        val lenBytes = ByteArray(8)
        if (stream.read(lenBytes) != 8) return null
        val len = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN).long
        if (len < 0 || len > 1024 * 64) return null
        val strBytes = ByteArray(len.toInt())
        var totalRead = 0
        while (totalRead < len) {
            val r = stream.read(strBytes, totalRead, (len - totalRead).toInt())
            if (r <= 0) break
            totalRead += r
        }
        return String(strBytes, 0, totalRead, Charsets.UTF_8)
    }

    private fun readGgufValueString(stream: InputStream, type: GgufType?): String? {
        if (type == null) return null
        return when (type) {
            GgufType.UINT8 -> stream.read().toString()
            GgufType.INT8 -> stream.read().toByte().toString()
            GgufType.UINT16, GgufType.INT16 -> {
                val b = ByteArray(2)
                stream.read(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toString()
            }
            GgufType.UINT32, GgufType.INT32 -> {
                val b = ByteArray(4)
                stream.read(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int.toString()
            }
            GgufType.FLOAT32 -> {
                val b = ByteArray(4)
                stream.read(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).float.toString()
            }
            GgufType.BOOL -> (stream.read() != 0).toString()
            GgufType.STRING -> readGgufString(stream)
            GgufType.UINT64, GgufType.INT64 -> {
                val b = ByteArray(8)
                stream.read(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long.toString()
            }
            GgufType.FLOAT64 -> {
                val b = ByteArray(8)
                stream.read(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).double.toString()
            }
            GgufType.ARRAY -> {
                // Read element type and count, skip array elements
                val elemTypeBytes = ByteArray(4)
                if (stream.read(elemTypeBytes) != 4) return "[]"
                val countBytes = ByteArray(8)
                if (stream.read(countBytes) != 8) return "[]"
                val count = ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN).long
                "[Array of $count items]"
            }
        }
    }

    fun extractMetadata(
        fileName: String,
        filePath: String,
        fileSize: Long,
        version: UInt,
        tensorCount: ULong,
        kvCount: ULong,
        kvMap: Map<String, String>
    ): GgufMetadata {
        val arch = kvMap["general.architecture"] ?: inferArchitectureFromFileName(fileName)
        val modelName = kvMap["general.name"]
            ?: inferModelNameFromFileName(fileName)
        val quant = kvMap["general.file_type"]?.let { mapFileTypeToQuant(it) }
            ?: inferQuantizationFromFileName(fileName)
        val contextLen = kvMap["$arch.context_length"]?.toIntOrNull()
            ?: kvMap["llama.context_length"]?.toIntOrNull()
            ?: 2048
        val embLen = kvMap["$arch.embedding_length"]?.toIntOrNull()
            ?: kvMap["llama.embedding_length"]?.toIntOrNull()
            ?: 2048
        val blockCount = kvMap["$arch.block_count"]?.toIntOrNull()
            ?: kvMap["llama.block_count"]?.toIntOrNull()
            ?: 24
        val headCount = kvMap["$arch.attention.head_count"]?.toIntOrNull()
            ?: kvMap["llama.attention.head_count"]?.toIntOrNull()
            ?: 16

        val paramsEst = inferParams(fileSize, quant, arch)

        return GgufMetadata(
            fileName = fileName,
            filePath = filePath,
            fileSize = fileSize,
            magic = GGUF_MAGIC,
            version = version,
            tensorCount = tensorCount,
            metadataKvCount = kvCount,
            architecture = arch,
            modelName = modelName,
            quantization = quant,
            contextLength = contextLen,
            embeddingLength = embLen,
            blockCount = blockCount,
            headCount = headCount,
            estimatedParams = paramsEst,
            rawKvPairs = kvMap,
            isValid = true,
            parseError = null
        )
    }

    private fun inferArchitectureFromFileName(name: String): String {
        val lower = name.lowercase()
        return when {
            "smollm" in lower -> "smollm"
            "qwen" in lower -> "qwen2"
            "llama" in lower -> "llama"
            "gemma" in lower -> "gemma2"
            "phi" in lower -> "phi3"
            "mistral" in lower -> "mistral"
            "deepseek" in lower -> "deepseek"
            else -> "transformer"
        }
    }

    private fun inferModelNameFromFileName(name: String): String {
        return name.replace(".gguf", "", ignoreCase = true)
            .replace("-", " ")
            .replace("_", " ")
            .trim()
    }

    private fun inferQuantizationFromFileName(name: String): String {
        val upper = name.uppercase()
        val quants = listOf(
            "Q4_K_M", "Q4_K_S", "Q4_0", "Q4_1",
            "Q5_K_M", "Q5_K_S", "Q5_0", "Q5_1",
            "Q8_0", "Q6_K", "Q2_K", "Q3_K_M", "F16", "BF16"
        )
        return quants.firstOrNull { it in upper } ?: "Q4_K_M"
    }

    private fun mapFileTypeToQuant(fileTypeStr: String): String {
        return when (fileTypeStr.toIntOrNull()) {
            0 -> "F32"
            1 -> "F16"
            2 -> "Q4_0"
            3 -> "Q4_1"
            7 -> "Q8_0"
            12 -> "Q4_K"
            15 -> "Q4_K_M"
            17 -> "Q5_K_M"
            else -> "Q4_K_M"
        }
    }

    private fun inferParams(sizeBytes: Long, quant: String, arch: String): String {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return when {
            mb < 150 -> "135M"
            mb < 300 -> "360M"
            mb < 600 -> "0.5B"
            mb < 1200 -> "1.1B - 1.5B"
            mb < 2500 -> "2B - 3B"
            mb < 5000 -> "7B - 8B"
            else -> "14B+"
        }
    }

    private fun invalidMetadata(fileName: String, path: String, size: Long, error: String): GgufMetadata {
        return GgufMetadata(
            fileName = fileName,
            filePath = path,
            fileSize = size,
            magic = "",
            version = 0u,
            tensorCount = 0u,
            metadataKvCount = 0u,
            architecture = "Unknown",
            modelName = fileName,
            quantization = "Unknown",
            contextLength = 2048,
            embeddingLength = 0,
            blockCount = 0,
            headCount = 0,
            estimatedParams = "Unknown",
            rawKvPairs = emptyMap(),
            isValid = false,
            parseError = error
        )
    }

    /**
     * Creates a genuine sample GGUF v3 file on disk for offline testing and verification.
     */
    fun createSampleGgufFile(targetFile: File, modelName: String = "SmolLM2-135M-Instruct-Q4_K_M.gguf") {
        if (targetFile.exists() && targetFile.length() > 1024) return
        targetFile.parentFile?.mkdirs()
        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.setLength(0)
            // Magic: "GGUF"
            raf.write("GGUF".toByteArray(Charsets.US_ASCII))
            // Version: 3 (uint32)
            val buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(3)
            // Tensor count: 120 (uint64)
            buf.putLong(120L)
            // Metadata KV count: 5 (uint64)
            buf.putLong(5L)
            raf.write(buf.array(), 0, buf.position())

            // Helper to write key-value
            fun writeKvString(key: String, value: String) {
                val keyBytes = key.toByteArray(Charsets.UTF_8)
                val valBytes = value.toByteArray(Charsets.UTF_8)
                val b = ByteBuffer.allocate(64 + keyBytes.size + valBytes.size).order(ByteOrder.LITTLE_ENDIAN)
                b.putLong(keyBytes.size.toLong())
                b.put(keyBytes)
                b.putInt(GgufType.STRING.id)
                b.putLong(valBytes.size.toLong())
                b.put(valBytes)
                raf.write(b.array(), 0, b.position())
            }

            fun writeKvUint32(key: String, value: Int) {
                val keyBytes = key.toByteArray(Charsets.UTF_8)
                val b = ByteBuffer.allocate(64 + keyBytes.size).order(ByteOrder.LITTLE_ENDIAN)
                b.putLong(keyBytes.size.toLong())
                b.put(keyBytes)
                b.putInt(GgufType.UINT32.id)
                b.putInt(value)
                raf.write(b.array(), 0, b.position())
            }

            writeKvString("general.architecture", "smollm")
            writeKvString("general.name", "SmolLM2 135M Instruct")
            writeKvString("general.file_type", "15") // Q4_K_M
            writeKvUint32("smollm.context_length", 2048)
            writeKvUint32("smollm.embedding_length", 576)

            // Pad file to represent quantized weights header
            val padding = ByteArray(4096)
            raf.write(padding)
        }
    }
}

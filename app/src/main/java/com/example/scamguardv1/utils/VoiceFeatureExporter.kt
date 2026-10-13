package com.example.scamguardv1.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.scamguardv1.domain.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class VoiceFeatureExporter(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val encryptionManager = EncryptionManager(context)

    fun exportDetailedReport(
        audioFile: File,
        detailedWindows: List<Map<String, Double?>>,
        durationSeconds: Double,
        sampleRate: Int
    ): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileNameBase = "voice_enrollment_$timestamp"
        
        val allFeatureNames = detailedWindows.flatMap { it.keys }.distinct().sorted()
        
        // Calculate aggregates
        val aggregate = calculateAggregates(detailedWindows, allFeatureNames)
        
        val windowSets = detailedWindows.mapIndexed { index, features ->
            WindowFeatureSet(index + 1, features)
        }
        
        val report = VoiceReportPayload(
            analysisType = "V12 acoustic + V14.2 expanded feature extraction (Kotlin Port)",
            input = VoiceInputMetadata(
                path = audioFile.absolutePath,
                filename = audioFile.name,
                extension = audioFile.extension,
                sizeBytes = audioFile.length(),
                modifiedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(audioFile.lastModified()))
            ),
            audio = VoiceAudioMetadata(
                durationSeconds = durationSeconds,
                analysisWindows = detailedWindows.size,
                featureCount = allFeatureNames.size,
                sampleRate = sampleRate
            ),
            features = VoiceFeatureReport(
                names = allFeatureNames,
                aggregate = aggregate,
                windows = windowSets
            )
        )
        
        val jsonString = gson.toJson(report)
        
        // Save both files (locally encrypted)
        val tempDir = File(context.cacheDir, "encrypted_temp").apply { mkdirs() }
        
        val encryptedAudioFile = File(tempDir, "${fileNameBase}.mp3.enc")
        encryptionManager.encryptFile(audioFile, encryptedAudioFile)
        saveFile(encryptedAudioFile.readBytes(), "${fileNameBase}.mp3.enc", "application/octet-stream")
        
        val jsonFile = File(tempDir, "${fileNameBase}_features.json")
        jsonFile.writeText(jsonString)
        val encryptedJsonFile = File(tempDir, "${fileNameBase}_features.json.enc")
        encryptionManager.encryptFile(jsonFile, encryptedJsonFile)
        
        val uri = saveFile(encryptedJsonFile.readBytes(), "${fileNameBase}_voice_features.json.enc", "application/octet-stream")
        
        // Cleanup temp
        tempDir.deleteRecursively()
        
        return uri
    }

    private fun calculateAggregates(
        windows: List<Map<String, Double?>>,
        names: List<String>
    ): Map<String, AggregateStats> {
        val result = mutableMapOf<String, AggregateStats>()
        
        for (name in names) {
            val values = windows.mapNotNull { it[name] }
            if (values.isNotEmpty()) {
                result[name] = computeRobustStats(values)
            }
        }
        
        return result
    }

    private fun computeRobustStats(values: List<Double>): AggregateStats {
        val sorted = values.sorted()
        val n = values.size
        
        fun percentile(p: Double): Double {
            val idx = (p / 100.0 * (n - 1))
            val low = idx.toInt()
            val high = (idx + 1).toInt().coerceAtMost(n - 1)
            val weight = idx - low
            return sorted[low] * (1.0 - weight) + sorted[high] * weight
        }

        val q10 = percentile(10.0)
        val q25 = percentile(25.0)
        val q50 = percentile(50.0)
        val q75 = percentile(75.0)
        val q90 = percentile(90.0)
        
        val mean = values.average()
        var sumSqDiff = 0.0
        var sumCubeDiff = 0.0
        var sumQuadDiff = 0.0
        
        for (v in values) {
            val diff = v - mean
            sumSqDiff += diff.pow(2)
            sumCubeDiff += diff.pow(3)
            sumQuadDiff += diff.pow(4)
        }
        
        val variance = sumSqDiff / n
        val std = sqrt(variance)
        
        val skew = if (std > 1e-12) (sumCubeDiff / n) / std.pow(3) else 0.0
        val kurtosis = if (std > 1e-12) (sumQuadDiff / n) / std.pow(4) - 3.0 else 0.0
        
        return AggregateStats(
            n = n,
            q10 = q10,
            q25 = q25,
            q50 = q50,
            q75 = q75,
            q90 = q90,
            iqr = q75 - q25,
            mean = mean,
            std = std,
            skew = skew,
            kurtosis = kurtosis,
            min = sorted.first(),
            max = sorted.last(),
            range = sorted.last() - sorted.first()
        )
    }

    private fun saveFile(data: ByteArray, fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(data, fileName, mimeType)
        } else {
            saveToExternalStorageLegacy(data, fileName)
        }
    }

    private fun saveToMediaStore(data: ByteArray, fileName: String, mimeType: String): Uri? {
        val relativePath = "Download/Verity/data/encrypted"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val resolver = context.contentResolver
        // Use EXTERNAL_CONTENT_URI for generic files in Downloads if Downloads is not available
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
             MediaStore.Files.getContentUri("external")
        }
        
        val uri = resolver.insert(collection, contentValues)
        
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(data)
            }
        }
        
        return uri
    }

    private fun saveToExternalStorageLegacy(data: ByteArray, fileName: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val verityDir = File(downloadsDir, "Verity/data/encrypted")
        if (!verityDir.exists()) verityDir.mkdirs()
        
        val file = File(verityDir, fileName)
        try {
            FileOutputStream(file).use { it.write(data) }
            return Uri.fromFile(file)
        } catch (e: Exception) {
            Logger.e("Failed to save file legacy: $fileName", e)
            return null
        }
    }
}

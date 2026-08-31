package com.example.scamguardv1.data

import android.content.Context
import android.net.Uri

import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.utils.Logger
import kotlin.math.abs

class VoiceAnalyzer(private val context: Context) {

    fun getAudioData(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            Logger.e("Failed to read audio data from URI: $uri", e)
            null
        }
    }

    /**
     * Extracts meaningful heuristics from raw audio bytes.
     * Uses primitive arrays to avoid OOM on large files.
     */
    fun extractFeatures(audioData: ByteArray): VoiceFeatures {
        if (audioData.size < 2) return VoiceFeatures(0f, 0f, 0f, 0f)

        // Convert to short samples (assuming 16-bit PCM)
        val numSamples = audioData.size / 2
        val samples = FloatArray(numSamples)
        
        for (i in 0 until numSamples) {
            val byteIdx = i * 2
            val sample = ((audioData[byteIdx + 1].toInt() shl 8) or (audioData[byteIdx].toInt() and 0xFF)).toShort()
            samples[i] = sample.toFloat() / 32768f
        }

        // 1. Zero Crossing Rate
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            val prev = samples[i - 1]
            val curr = samples[i]
            if ((prev > 0 && curr <= 0) || (prev < 0 && curr >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / samples.size.coerceAtLeast(1)

        // 2. Spectral Centroid simulation
        var weightedSum = 0.0 // Use Double for intermediate sums to prevent precision loss/overflow
        var totalSum = 0.0
        for (i in samples.indices) {
            val mag = abs(samples[i]).toDouble()
            weightedSum += mag * i
            totalSum += mag
        }
        val centroid = (weightedSum / totalSum.coerceAtLeast(0.001)) / samples.size.coerceAtLeast(1)

        // 3. Energy Stability
        val windowSize = 100
        val numWindows = samples.size / windowSize
        if (numWindows == 0) return VoiceFeatures(centroid.toFloat(), zcr, 0f, (zcr * 2f).coerceIn(0f, 1f))

        val energies = FloatArray(numWindows)
        var sumEnergy = 0.0
        for (w in 0 until numWindows) {
            var energy = 0f
            val start = w * windowSize
            for (j in 0 until windowSize) {
                val s = samples[start + j]
                energy += s * s
            }
            energies[w] = energy
            sumEnergy += energy
        }
        
        val avgEnergy = (sumEnergy / numWindows).toFloat()
        var varianceSum = 0.0
        for (energy in energies) {
            val diff = energy - avgEnergy
            varianceSum += diff * diff
        }
        
        val variance = (varianceSum / numWindows).toFloat()
        val energyStability = 1f - (variance / avgEnergy.coerceAtLeast(0.001f)).coerceIn(0f, 1f)

        return VoiceFeatures(
            spectralCentroid = centroid.toFloat(),
            zeroCrossingRate = zcr,
            energyStability = energyStability,
            harmonicsRatio = (zcr * 2f).coerceIn(0f, 1f)
        )
    }
}

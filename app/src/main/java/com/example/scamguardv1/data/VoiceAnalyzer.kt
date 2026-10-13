package com.example.scamguardv1.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.scamguardv1.domain.model.VoiceFeatures
import com.example.scamguardv1.utils.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class VoiceAnalyzer(private val context: Context) {

    private val DEFAULT_SAMPLE_RATE = 22050 // Matching common librosa defaults or Python script v12

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
     * Extracts raw PCM samples from an audio file using MediaExtractor.
     */
    fun extractPcmSamples(uri: Uri): Pair<FloatArray, Int>? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) return null
            
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            
            extractor.selectTrack(trackIndex)
            
            val allSamples = mutableListOf<Float>()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                buffer.rewind()
                val numShorts = sampleSize / 2
                for (j in 0 until numShorts) {
                    val s = buffer.short
                    allSamples.add(s.toFloat() / 32768f)
                }
                extractor.advance()
            }
            return Pair(allSamples.toFloatArray(), sampleRate)
        } catch (e: Exception) {
            Logger.e("Failed to extract PCM from $uri", e)
            return null
        } finally {
            extractor.release()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    /**
     * Extracts meaningful heuristics from raw audio bytes (assumed PCM).
     */
    fun extractFeatures(audioData: ByteArray): VoiceFeatures {
        if (audioData.size < 2) return VoiceFeatures(0f, 0f, 0f, 0f)

        val numSamples = audioData.size / 2
        val samples = FloatArray(numSamples)
        val buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until numSamples) {
            samples[i] = buffer.short.toFloat() / 32768f
        }

        return computeVoiceFeatures(samples)
    }

    fun computeVoiceFeatures(samples: FloatArray): VoiceFeatures {
        if (samples.isEmpty()) return VoiceFeatures(0f, 0f, 0f, 0f)

        // 1. Zero Crossing Rate
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] > 0 && samples[i] <= 0) || (samples[i - 1] < 0 && samples[i] >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / samples.size.coerceAtLeast(1)

        // 2. Spectral Centroid simulation
        var weightedSum = 0.0
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

    /**
     * Detailed windowed analysis for the V14.2 expanded feature set.
     */
    fun extractDetailedWindows(samples: FloatArray, sampleRate: Int, windowSeconds: Double = 1.0): List<Map<String, Double?>> {
        val windowSize = (windowSeconds * sampleRate).toInt()
        val results = mutableListOf<Map<String, Double?>>()
        
        var i = 0
        while (i + windowSize <= samples.size) {
            val window = samples.sliceArray(i until i + windowSize)
            results.add(extractWindowDetailedFeatures(window, sampleRate))
            i += windowSize
        }
        
        return results
    }

    private fun extractWindowDetailedFeatures(samples: FloatArray, sampleRate: Int): Map<String, Double?> {
        val features = mutableMapOf<String, Double?>()
        
        // --- Pitch (F0) Proxy via Autocorrelation ---
        val f0 = estimateF0(samples, sampleRate)
        features["new.pitch.f0_median"] = f0

        // --- Energy ---
        var sumSq = 0.0
        for (s in samples) sumSq += s * s
        val rms = sqrt(sumSq / samples.size)
        val rmsDb = 20.0 * log10(rms.coerceAtLeast(1e-8))
        features["new.energy.rms_q50"] = rms
        features["new.energy.rms_db_q50"] = rmsDb

        // --- Spectral Proxies ---
        var zeroCrossings = 0
        for (j in 1 until samples.size) {
            if ((samples[j - 1] > 0 && samples[j] <= 0) || (samples[j - 1] < 0 && samples[j] >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toDouble() / samples.size
        features["new.rhythm.voiced_fraction"] = if (rms > 0.01) 1.0 else 0.0
        features["new.rhythm.zero_crossing_std"] = zcr // Simplified proxy

        // --- Formants Proxies (Simplified) ---
        features["formants.F1"] = 500.0 + (zcr * 1000.0)
        features["formants.F2"] = 1500.0 + (rms * 500.0)
        features["formants.F3"] = 2500.0

        return features
    }

    private fun estimateF0(samples: FloatArray, sampleRate: Int): Double? {
        val n = samples.size
        val r = DoubleArray(n)
        for (tau in 0 until n / 2) {
            var sum = 0.0
            for (i in 0 until n - tau) {
                sum += samples[i] * samples[i + tau]
            }
            r[tau] = sum
        }
        
        val minTau = sampleRate / 500
        val maxTau = sampleRate / 50
        
        var maxVal = -1.0
        var peakTau = -1
        for (tau in minTau..maxTau) {
            if (tau >= r.size) break
            if (r[tau] > maxVal) {
                maxVal = r[tau]
                peakTau = tau
            }
        }
        
        return if (peakTau > 0) sampleRate.toDouble() / peakTau else null
    }
}

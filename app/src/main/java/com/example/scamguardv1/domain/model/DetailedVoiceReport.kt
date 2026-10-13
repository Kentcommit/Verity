package com.example.scamguardv1.domain.model

import com.google.gson.annotations.SerializedName

data class VoiceReportPayload(
    @SerializedName("version") val version: String = "1.0",
    @SerializedName("analysis_type") val analysisType: String,
    @SerializedName("production_model_modified") val productionModelModified: Boolean = false,
    @SerializedName("input") val input: VoiceInputMetadata,
    @SerializedName("audio") val audio: VoiceAudioMetadata,
    @SerializedName("features") val features: VoiceFeatureReport,
)

data class VoiceInputMetadata(
    @SerializedName("path") val path: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("extension") val extension: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
    @SerializedName("modified_time") val modifiedTime: String
)

data class VoiceAudioMetadata(
    @SerializedName("duration_seconds") val durationSeconds: Double,
    @SerializedName("analysis_windows") val analysisWindows: Int,
    @SerializedName("feature_count") val featureCount: Int,
    @SerializedName("sample_rate") val sampleRate: Int
)

data class VoiceFeatureReport(
    @SerializedName("names") val names: List<String>,
    @SerializedName("aggregate") val aggregate: Map<String, AggregateStats>,
    @SerializedName("windows") val windows: List<WindowFeatureSet>
)

data class WindowFeatureSet(
    @SerializedName("window_index") val windowIndex: Int,
    @SerializedName("features") val features: Map<String, Double?>
)

data class AggregateStats(
    @SerializedName("n") val n: Int,
    @SerializedName("q10") val q10: Double,
    @SerializedName("q25") val q25: Double,
    @SerializedName("q50") val q50: Double,
    @SerializedName("q75") val q75: Double,
    @SerializedName("q90") val q90: Double,
    @SerializedName("iqr") val iqr: Double,
    @SerializedName("mean") val mean: Double,
    @SerializedName("std") val std: Double,
    @SerializedName("skew") val skew: Double,
    @SerializedName("kurtosis") val kurtosis: Double,
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double,
    @SerializedName("range") val range: Double
)

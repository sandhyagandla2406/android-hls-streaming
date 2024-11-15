package com.canadore.hlsstreaming.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single streamable video from the backend catalog.
 */
data class StreamItem(
    @SerializedName("id")          val id: String,
    @SerializedName("title")       val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("duration")    val durationSeconds: Int,
    @SerializedName("thumbnail")   val thumbnailUrl: String,
    @SerializedName("manifest_url") val manifestUrl: String,
    @SerializedName("drm_type")    val drmType: DrmType = DrmType.NONE,
    @SerializedName("license_url") val licenseUrl: String? = null,
    @SerializedName("qualities")   val availableQualities: List<StreamQuality> = emptyList()
)

/**
 * Represents a single quality rendition inside the HLS master playlist.
 */
data class StreamQuality(
    @SerializedName("label")     val label: String,       // e.g. "1080p", "720p", "360p"
    @SerializedName("bandwidth") val bandwidth: Long,     // bits per second
    @SerializedName("width")     val width: Int,
    @SerializedName("height")    val height: Int,
    @SerializedName("url")       val url: String
)

/**
 * Supported DRM types.
 */
enum class DrmType {
    NONE,
    WIDEVINE,
    CLEARKEY
}

/**
 * Network condition profile used by the simulator.
 */
data class NetworkProfile(
    val name: String,
    val displayName: String,
    val bandwidthBps: Long,
    val latencyMs: Int,
    val packetLossPct: Float
) {
    companion object {
        val WIFI = NetworkProfile("wifi",  "Wi-Fi",    50_000_000L, 5,   0.0f)
        val LTE  = NetworkProfile("lte",   "4G LTE",   20_000_000L, 30,  0.1f)
        val G3   = NetworkProfile("3g",    "3G",        2_000_000L, 100, 1.0f)
        val POOR = NetworkProfile("poor",  "Poor",        500_000L, 300, 5.0f)

        val ALL = listOf(WIFI, LTE, G3, POOR)
    }
}

/**
 * Playback event captured during a session for the diagnostics overlay.
 */
data class PlaybackEvent(
    val timestampMs: Long = System.currentTimeMillis(),
    val type: EventType,
    val detail: String
) {
    enum class EventType {
        QUALITY_SWITCH, BUFFER_START, BUFFER_END, ERROR, DRM_KEY_FETCHED, TRACK_SELECTED
    }
}

/**
 * Real-time playback stats shown in the diagnostics overlay.
 */
data class PlaybackStats(
    val currentBitrateKbps: Int = 0,
    val estimatedBandwidthKbps: Int = 0,
    val bufferMs: Long = 0,
    val droppedFrames: Int = 0,
    val currentQuality: String = "-",
    val networkProfile: String = "wifi"
)

/**
 * Backend health response.
 */
data class HealthResponse(
    @SerializedName("status")  val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("streams") val streamCount: Int
)

/**
 * Wrapper for API list responses.
 */
data class StreamListResponse(
    @SerializedName("streams") val streams: List<StreamItem>,
    @SerializedName("total")   val total: Int
)

package com.canadore.hlsstreaming.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.canadore.hlsstreaming.model.DrmType
import com.canadore.hlsstreaming.model.PlaybackEvent
import com.canadore.hlsstreaming.model.PlaybackStats
import com.canadore.hlsstreaming.model.StreamItem
import com.canadore.hlsstreaming.network.NetworkSimulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "PlayerManager"

/**
 * Wraps ExoPlayer setup for HLS adaptive streaming.
 *
 * Responsibilities:
 * - Building the [ExoPlayer] instance with an [AdaptiveTrackSelection] factory
 * - Attaching an OkHttp-backed data source that respects the active [NetworkSimulator] profile
 * - Wiring DRM via [DrmSessionHandler]
 * - Emitting real-time [PlaybackStats] and [PlaybackEvent] flows consumed by the UI
 */
@UnstableApi
class PlayerManager(private val context: Context) {

    // ── Exposed state ─────────────────────────────────────────────────────────
    private val _stats  = MutableStateFlow(PlaybackStats())
    val stats: StateFlow<PlaybackStats> = _stats

    private val _events = MutableStateFlow<PlaybackEvent?>(null)
    val events: StateFlow<PlaybackEvent?> = _events

    // ── Internals ─────────────────────────────────────────────────────────────
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var bandwidthMeter: DefaultBandwidthMeter? = null
    private var statsCollector: PlaybackStatsCollector? = null

    /**
     * Build and return a fully configured [ExoPlayer].
     * Call this every time the network profile changes so the data source is recreated.
     */
    fun buildPlayer(streamItem: StreamItem): ExoPlayer {
        release()

        val profile = NetworkSimulator.activeProfile.value
        val okHttpClient = NetworkSimulator.buildClientForProfile(profile)

        // Bandwidth meter — feeds the ABR algorithm with real throughput samples
        bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(profile.bandwidthBps)
            .build()

        // Track selector using adaptive selection factory
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(context, trackSelectionFactory).apply {
            // Start from lowest quality and ramp up (safer for demos)
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        // OkHttp-backed data source factory (honours throttle interceptor)
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(mapOf("User-Agent" to "HLSStreamingPOC/1.0"))

        // HLS source
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(buildMediaItem(streamItem))

        val newPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .setBandwidthMeter(bandwidthMeter!!)
            .build()
            .also { it.setMediaSource(hlsMediaSource) }

        // Stats / event listener
        statsCollector = PlaybackStatsCollector(newPlayer, bandwidthMeter!!, trackSelector!!) { stats, event ->
            _stats.value = stats.copy(networkProfile = profile.name)
            event?.let { _events.value = it }
        }
        newPlayer.addListener(statsCollector!!)
        newPlayer.prepare()

        player = newPlayer
        Log.d(TAG, "ExoPlayer built for profile=${profile.name} stream=${streamItem.id}")
        return newPlayer
    }

    /** Attach the player to a [PlayerView]. */
    fun attachView(playerView: PlayerView) {
        playerView.player = player
    }

    /** Detach from view (e.g. on pause/stop). */
    fun detachView(playerView: PlayerView) {
        playerView.player = null
    }

    /** Release all resources. */
    fun release() {
        statsCollector?.let { player?.removeListener(it) }
        player?.release()
        player = null
        trackSelector = null
        bandwidthMeter = null
        statsCollector = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildMediaItem(stream: StreamItem): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(buildManifestUrl(stream.manifestUrl)))

        // DRM configuration
        when (stream.drmType) {
            DrmType.WIDEVINE -> {
                stream.licenseUrl?.let { url ->
                    builder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(url)
                            .build()
                    )
                }
            }
            DrmType.CLEARKEY -> {
                stream.licenseUrl?.let { url ->
                    builder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                            .setLicenseUri(url)
                            .build()
                    )
                }
            }
            DrmType.NONE -> { /* no DRM */ }
        }

        return builder.build()
    }

    private fun buildManifestUrl(rawUrl: String): String {
        // If the manifest URL is relative, prepend the backend base URL
        return if (rawUrl.startsWith("http")) rawUrl
        else "${NetworkSimulator.activeProfile.value.let { com.canadore.hlsstreaming.network.NetworkConfig.BASE_URL }}$rawUrl"
    }
}

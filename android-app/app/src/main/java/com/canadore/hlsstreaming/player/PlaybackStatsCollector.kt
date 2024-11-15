package com.canadore.hlsstreaming.player

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.canadore.hlsstreaming.model.PlaybackEvent
import com.canadore.hlsstreaming.model.PlaybackStats

private const val TAG = "StatsCollector"

/**
 * Listens to [ExoPlayer] analytics events and synthesises a [PlaybackStats]
 * snapshot plus discrete [PlaybackEvent]s that are forwarded to the UI layer.
 */
@UnstableApi
class PlaybackStatsCollector(
    private val player: ExoPlayer,
    private val bandwidthMeter: DefaultBandwidthMeter,
    private val trackSelector: DefaultTrackSelector,
    private val onUpdate: (PlaybackStats, PlaybackEvent?) -> Unit
) : Player.Listener, AnalyticsListener {

    private var lastQualityLabel = "-"
    private var lastBitrateKbps = 0

    // ── Player.Listener ───────────────────────────────────────────────────────

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> emitEvent(PlaybackEvent.EventType.BUFFER_START, "Buffering…")
            Player.STATE_READY     -> emitEvent(PlaybackEvent.EventType.BUFFER_END,   "Playback ready")
            else -> {}
        }
        pushStats()
    }

    override fun onTracksChanged(tracks: Tracks) {
        val videoTrack = tracks.groups
            .filter { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
            .firstOrNull { it.isSelected }

        val format = videoTrack?.getTrackFormat(
            (0 until videoTrack.length).firstOrNull { videoTrack.isTrackSelected(it) } ?: 0
        )

        format?.let { f ->
            val label = "${f.height}p"
            val kbps  = f.bitrate / 1000
            if (label != lastQualityLabel || kbps != lastBitrateKbps) {
                lastQualityLabel = label
                lastBitrateKbps  = kbps
                emitEvent(PlaybackEvent.EventType.QUALITY_SWITCH, "→ $label @ ${kbps}kbps")
                Log.d(TAG, "Quality switched to $label @ ${kbps}kbps")
            }
        }
        pushStats()
    }

    override fun onPlayerError(error: PlaybackException) {
        emitEvent(PlaybackEvent.EventType.ERROR, error.message ?: "Unknown error")
        Log.e(TAG, "Player error: ${error.message}")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun pushStats() {
        val estimatedBw = (bandwidthMeter.bitrateEstimate / 1000).toInt()
        val bufferedMs  = player.totalBufferedDuration
        val dropped     = player.videoDecoderCounters?.droppedBufferCount ?: 0

        onUpdate(
            PlaybackStats(
                currentBitrateKbps      = lastBitrateKbps,
                estimatedBandwidthKbps  = estimatedBw,
                bufferMs                = bufferedMs,
                droppedFrames           = dropped,
                currentQuality          = lastQualityLabel
            ),
            null
        )
    }

    private fun emitEvent(type: PlaybackEvent.EventType, detail: String) {
        onUpdate(
            PlaybackStats(
                currentBitrateKbps     = lastBitrateKbps,
                estimatedBandwidthKbps = (bandwidthMeter.bitrateEstimate / 1000).toInt(),
                bufferMs               = player.totalBufferedDuration,
                currentQuality         = lastQualityLabel
            ),
            PlaybackEvent(type = type, detail = detail)
        )
    }
}

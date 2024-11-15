package com.canadore.hlsstreaming.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.canadore.hlsstreaming.databinding.ActivityPlayerBinding
import com.canadore.hlsstreaming.model.DrmType
import com.canadore.hlsstreaming.model.StreamItem
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STREAM_ID     = "stream_id"
        const val EXTRA_STREAM_TITLE  = "stream_title"
        const val EXTRA_MANIFEST_URL  = "manifest_url"
        const val EXTRA_DRM_TYPE      = "drm_type"
        const val EXTRA_LICENSE_URL   = "license_url"
    }

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stream = extractStreamFromIntent()
        title = stream.title

        setupDiagnosticsToggle()
        observeStats()
        observeEvents()

        viewModel.initPlayer(applicationContext, stream, binding.playerView)
    }

    // ── Intent extraction ─────────────────────────────────────────────────────

    private fun extractStreamFromIntent() = StreamItem(
        id           = intent.getStringExtra(EXTRA_STREAM_ID)    ?: "unknown",
        title        = intent.getStringExtra(EXTRA_STREAM_TITLE) ?: "Stream",
        description  = "",
        durationSeconds = 0,
        thumbnailUrl = "",
        manifestUrl  = intent.getStringExtra(EXTRA_MANIFEST_URL) ?: "",
        drmType      = DrmType.valueOf(intent.getStringExtra(EXTRA_DRM_TYPE) ?: "NONE"),
        licenseUrl   = intent.getStringExtra(EXTRA_LICENSE_URL)
    )

    // ── Diagnostics overlay ───────────────────────────────────────────────────

    private fun setupDiagnosticsToggle() {
        binding.btnToggleDiagnostics.setOnClickListener {
            val visible = binding.layoutDiagnostics.visibility == View.VISIBLE
            binding.layoutDiagnostics.visibility = if (visible) View.GONE else View.VISIBLE
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                binding.tvBitrate.text         = "Bitrate: ${stats.currentBitrateKbps} kbps"
                binding.tvBandwidth.text       = "Est. BW: ${stats.estimatedBandwidthKbps} kbps"
                binding.tvBuffer.text          = "Buffer: ${stats.bufferMs / 1000}s"
                binding.tvQuality.text         = "Quality: ${stats.currentQuality}"
                binding.tvDroppedFrames.text   = "Dropped: ${stats.droppedFrames}"
                binding.tvNetworkProfile.text  = "Profile: ${stats.networkProfile}"
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                event ?: return@collect
                binding.tvLastEvent.text = "[${event.type.name}] ${event.detail}"
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStart()  { super.onStart();  viewModel.onStart(binding.playerView) }
    override fun onStop()   { super.onStop();   viewModel.onStop(binding.playerView) }
    override fun onDestroy(){ super.onDestroy(); viewModel.onDestroy() }
}

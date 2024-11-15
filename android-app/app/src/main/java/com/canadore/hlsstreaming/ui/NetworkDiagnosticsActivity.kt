package com.canadore.hlsstreaming.ui

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.canadore.hlsstreaming.R
import com.canadore.hlsstreaming.databinding.ActivityNetworkDiagnosticsBinding
import com.canadore.hlsstreaming.model.NetworkProfile
import com.canadore.hlsstreaming.network.NetworkSimulator
import kotlinx.coroutines.launch

/**
 * Displays the active network simulation profile and lets the user switch profiles.
 * Also shows a table summarising each profile's bandwidth / latency characteristics.
 */
class NetworkDiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkDiagnosticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = getString(R.string.title_diagnostics)
            setDisplayHomeAsUpEnabled(true)
        }

        setupRadioGroup()
        observeProfile()
    }

    private fun setupRadioGroup() {
        binding.rgNetworkProfiles.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            val profile = when (checkedId) {
                R.id.rbWifi -> NetworkProfile.WIFI
                R.id.rbLte  -> NetworkProfile.LTE
                R.id.rb3g   -> NetworkProfile.G3
                R.id.rbPoor -> NetworkProfile.POOR
                else        -> NetworkProfile.WIFI
            }
            NetworkSimulator.setProfile(profile)
        }
    }

    private fun observeProfile() {
        lifecycleScope.launch {
            NetworkSimulator.activeProfile.collect { profile ->
                // Sync radio selection
                val radioId = when (profile.name) {
                    "wifi" -> R.id.rbWifi
                    "lte"  -> R.id.rbLte
                    "3g"   -> R.id.rb3g
                    "poor" -> R.id.rbPoor
                    else   -> R.id.rbWifi
                }
                binding.rgNetworkProfiles.check(radioId)

                // Update info card
                binding.tvProfileName.text      = profile.displayName
                binding.tvBandwidth.text        = "Bandwidth: ${profile.bandwidthBps / 1_000_000} Mbps"
                binding.tvLatency.text          = "Latency: ${profile.latencyMs} ms"
                binding.tvPacketLoss.text       = "Packet loss: ${profile.packetLossPct}%"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

package com.canadore.hlsstreaming.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.canadore.hlsstreaming.R
import com.canadore.hlsstreaming.databinding.ActivityMainBinding
import com.canadore.hlsstreaming.model.NetworkProfile
import com.canadore.hlsstreaming.model.StreamItem
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var streamAdapter: StreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()
        observeNetworkProfile()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        streamAdapter = StreamAdapter { stream ->
            openPlayer(stream)
        }
        binding.rvStreams.apply {
            adapter = streamAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadStreams()
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = false
                when (state) {
                    is StreamListUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvStreams.visibility = View.GONE
                        binding.tvError.visibility = View.GONE
                    }
                    is StreamListUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvStreams.visibility = View.VISIBLE
                        binding.tvError.visibility = View.GONE
                        streamAdapter.submitList(state.streams)
                    }
                    is StreamListUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvStreams.visibility = View.GONE
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = state.message
                    }
                }
            }
        }
    }

    private fun observeNetworkProfile() {
        lifecycleScope.launch {
            viewModel.activeNetworkProfile.collect { profile ->
                binding.chipNetworkProfile.text = profile.displayName
                supportActionBar?.subtitle = "Network: ${profile.displayName}"
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun openPlayer(stream: StreamItem) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_ID,       stream.id)
            putExtra(PlayerActivity.EXTRA_STREAM_TITLE,    stream.title)
            putExtra(PlayerActivity.EXTRA_MANIFEST_URL,    stream.manifestUrl)
            putExtra(PlayerActivity.EXTRA_DRM_TYPE,        stream.drmType.name)
            putExtra(PlayerActivity.EXTRA_LICENSE_URL,     stream.licenseUrl)
        }
        startActivity(intent)
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_network_profile -> {
                showNetworkProfilePicker()
                true
            }
            R.id.action_diagnostics -> {
                startActivity(Intent(this, NetworkDiagnosticsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNetworkProfilePicker() {
        val profiles = NetworkProfile.ALL
        val labels   = profiles.map { "${it.displayName} (${it.bandwidthBps / 1_000_000}Mbps)" }.toTypedArray()
        val current  = profiles.indexOfFirst { it.name == viewModel.activeNetworkProfile.value.name }

        AlertDialog.Builder(this)
            .setTitle("Simulated Network Profile")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                viewModel.selectNetworkProfile(profiles[which])
                Toast.makeText(this, "Profile: ${profiles[which].displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

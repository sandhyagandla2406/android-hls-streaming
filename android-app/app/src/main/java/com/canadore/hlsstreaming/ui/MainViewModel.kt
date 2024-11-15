package com.canadore.hlsstreaming.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canadore.hlsstreaming.model.NetworkProfile
import com.canadore.hlsstreaming.model.StreamItem
import com.canadore.hlsstreaming.network.NetworkSimulator
import com.canadore.hlsstreaming.network.NetworkConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StreamListUiState {
    object Loading : StreamListUiState()
    data class Success(val streams: List<StreamItem>) : StreamListUiState()
    data class Error(val message: String) : StreamListUiState()
}

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<StreamListUiState>(StreamListUiState.Loading)
    val uiState: StateFlow<StreamListUiState> = _uiState

    val activeNetworkProfile: StateFlow<NetworkProfile> = NetworkSimulator.activeProfile

    init {
        loadStreams()
    }

    fun loadStreams() {
        viewModelScope.launch {
            _uiState.value = StreamListUiState.Loading
            try {
                val api = NetworkSimulator.buildApiServiceForCurrentProfile()
                val response = api.getStreams()
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = StreamListUiState.Success(body?.streams ?: emptyList())
                } else {
                    // Fallback: use mock data so the app works without a live backend
                    _uiState.value = StreamListUiState.Success(mockStreams())
                }
            } catch (e: Exception) {
                // No backend running — still show mock data
                _uiState.value = StreamListUiState.Success(mockStreams())
            }
        }
    }

    fun selectNetworkProfile(profile: NetworkProfile) {
        NetworkSimulator.setProfile(profile)
        loadStreams()
    }

    // ── Mock data (used when backend is unreachable) ──────────────────────────

    private fun mockStreams(): List<StreamItem> = listOf(
        StreamItem(
            id = "big-buck-bunny",
            title = "Big Buck Bunny",
            description = "Classic open-source animation — great for testing ABR.",
            durationSeconds = 596,
            thumbnailUrl = "https://peach.blender.org/wp-content/uploads/bbb-splash.png",
            manifestUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            drmType = com.canadore.hlsstreaming.model.DrmType.NONE
        ),
        StreamItem(
            id = "tears-of-steel",
            title = "Tears of Steel",
            description = "Sci-fi short — tests higher resolution tracks.",
            durationSeconds = 734,
            thumbnailUrl = "https://mango.blender.org/wp-content/uploads/2013/05/01_thom_celia_bridge.jpg",
            manifestUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            drmType = com.canadore.hlsstreaming.model.DrmType.NONE
        ),
        StreamItem(
            id = "drm-clearkey-demo",
            title = "ClearKey DRM Demo",
            description = "Demonstrates ClearKey DRM key exchange flow.",
            durationSeconds = 180,
            thumbnailUrl = "",
            manifestUrl = "https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd",
            drmType = com.canadore.hlsstreaming.model.DrmType.CLEARKEY,
            licenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth"
        )
    )
}

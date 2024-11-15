package com.canadore.hlsstreaming.network

import com.canadore.hlsstreaming.model.NetworkProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton that tracks the active [NetworkProfile] and rebuilds the
 * OkHttpClient / ExoPlayer data source whenever the profile changes.
 *
 * ViewModels observe [activeProfile] to update the UI badge.
 * [PlayerManager] observes it to recreate the ExoPlayer data source factory.
 */
object NetworkSimulator {

    private val _activeProfile = MutableStateFlow(NetworkProfile.WIFI)
    val activeProfile: StateFlow<NetworkProfile> = _activeProfile

    fun setProfile(profile: NetworkProfile) {
        _activeProfile.value = profile
    }

    /** Build an OkHttpClient pre-configured for the current profile. */
    fun buildClientForCurrentProfile(): okhttp3.OkHttpClient =
        buildClientForProfile(_activeProfile.value)

    fun buildClientForProfile(profile: NetworkProfile): okhttp3.OkHttpClient =
        NetworkConfig.buildOkHttpClient(
            bandwidthLimitBps = profile.bandwidthBps,
            latencyMs = profile.latencyMs
        )

    /** Convenience: build the API service wired to the current network profile. */
    fun buildApiServiceForCurrentProfile(): StreamingApiService =
        NetworkConfig.buildApiService(buildClientForCurrentProfile())
}

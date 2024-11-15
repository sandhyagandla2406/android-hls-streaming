package com.canadore.hlsstreaming.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.canadore.hlsstreaming.model.PlaybackEvent
import com.canadore.hlsstreaming.model.PlaybackStats
import com.canadore.hlsstreaming.model.StreamItem
import com.canadore.hlsstreaming.player.PlayerManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@UnstableApi
class PlayerViewModel : ViewModel() {

    private var playerManager: PlayerManager? = null

    val stats: StateFlow<PlaybackStats>
        get() = playerManager!!.stats

    val events: StateFlow<PlaybackEvent?>
        get() = playerManager!!.events

    fun initPlayer(context: Context, stream: StreamItem, playerView: PlayerView) {
        playerManager = PlayerManager(context).also { mgr ->
            val player = mgr.buildPlayer(stream)
            mgr.attachView(playerView)
            player.playWhenReady = true
        }
    }

    fun onStart(playerView: PlayerView) {
        playerManager?.attachView(playerView)
    }

    fun onStop(playerView: PlayerView) {
        playerManager?.detachView(playerView)
    }

    fun onDestroy() {
        playerManager?.release()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager?.release()
    }
}

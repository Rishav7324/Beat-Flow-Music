package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.MusicRepository
import com.example.data.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

data class MusicPlayerState(
    val songs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val currentPosition: Long = 0,
    val shuffleMode: Boolean = false,
    val searchQuery: String = "",
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    
    private val _uiState = MutableStateFlow(MusicPlayerState())
    val uiState: StateFlow<MusicPlayerState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), MusicPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val uri = mediaItem?.localConfiguration?.uri
                    val song = _uiState.value.songs.find { it.uri == uri }
                    _uiState.update { it.copy(currentSong = song) }
                }
            })
            
            // Periodically update progress
            viewModelScope.launch {
                while (true) {
                    val pos = mediaController?.currentPosition ?: 0
                    _uiState.update { it.copy(currentPosition = pos) }
                    kotlinx.coroutines.delay(1000)
                }
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun loadMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val songs = repository.getSongs()
            _uiState.update { it.copy(songs = songs, isLoading = false) }
        }
    }

    fun playSong(song: Song) {
        val controller = mediaController ?: return
        val currentSongs = _uiState.value.songs

        if (currentSongs.isNotEmpty()) {
            val mediaItems = currentSongs.map { s ->
                MediaItem.Builder()
                    .setUri(s.uri)
                    .setMediaId(s.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .build()
                    )
                    .build()
            }
            controller.setMediaItems(mediaItems)
            val index = currentSongs.indexOfFirst { it.id == song.id }
            if (index != -1) {
                controller.seekTo(index, C.TIME_UNSET)
            }
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayback() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipNext() {
        mediaController?.seekToNext()
    }

    fun skipPrevious() {
        mediaController?.seekToPrevious()
    }
    
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.let {
            val currentParams = it.playbackParameters
            it.playbackParameters = androidx.media3.common.PlaybackParameters(speed, currentParams.pitch)
        }
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setPitch(pitch: Float) {
        mediaController?.let {
            val currentParams = it.playbackParameters
            it.playbackParameters = androidx.media3.common.PlaybackParameters(currentParams.speed, pitch)
        }
        _uiState.update { it.copy(pitch = pitch) }
    }

    fun filterMusic(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}

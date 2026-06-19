package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.data.DatabaseProvider
import com.example.data.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // ExoPlayer natively handles gapless playback for applicable codecs when built like this
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                mediaItem?.mediaId?.let { trackId ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            DatabaseProvider.getDatabase(this@MusicPlaybackService)
                                .historyDao().incrementPlayCount(trackId.toLong(), System.currentTimeMillis())
                        } catch (e: Exception) {
                            // Ignored or handle
                        }
                    }
                }
            }
        })
            
        // Use custom callback for Android Auto browsing
        val callback = CustomMediaLibrarySessionCallback(this)
        
        mediaSession = MediaLibrarySession.Builder(this, player, callback)
            .build()
            
        // Register Headphone Disconnect Receiver
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    // Connect audio effects
    fun setupAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                setStrength(1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        equalizer?.release()
        bassBoost?.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

package com.pixelbuddy.app.data.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频播放器 — 基于 Media3 ExoPlayer。
 * 用于播放 TTS 合成后的音频。支持播放/暂停/停止/打断。
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null

    private val _playbackEvents = Channel<PlaybackEvent>(Channel.CONFLATED)
    val playbackEvents: Flow<PlaybackEvent> = _playbackEvents.receiveAsFlow()

    /**
     * 播放字节数组音频（TTS 返回的 MP3/WAV）。
     * 先将字节写入临时文件，再交给 ExoPlayer。
     */
    fun play(audioData: ByteArray, onComplete: () -> Unit = {}) {
        stop()

        val tempFile = File(context.cacheDir, "tts_output_${System.currentTimeMillis()}.mp3")
        FileOutputStream(tempFile).use { it.write(audioData) }

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(tempFile.toURI().toString()))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_ENDED -> {
                            onComplete()
                            _playbackEvents.trySend(PlaybackEvent.Finished)
                        }
                        Player.STATE_READY -> {
                            _playbackEvents.trySend(PlaybackEvent.Playing)
                        }
                        else -> {}
                    }
                }
            })
        }
    }

    /**
     * 停止播放并释放资源。
     */
    fun stop() {
        player?.apply {
            stop()
            release()
        }
        player = null
    }

    /**
     * 暂停播放（可恢复）。
     */
    fun pause() {
        player?.playWhenReady = false
    }

    /**
     * 恢复播放。
     */
    fun resume() {
        player?.playWhenReady = true
    }

    val isPlaying: Boolean get() = player?.isPlaying == true

    fun release() {
        stop()
    }
}

sealed class PlaybackEvent {
    data object Playing : PlaybackEvent()
    data object Finished : PlaybackEvent()
    data class Error(val message: String) : PlaybackEvent()
}

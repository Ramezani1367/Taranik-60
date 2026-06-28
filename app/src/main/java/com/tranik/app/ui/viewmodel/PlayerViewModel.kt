package com.tranik.app.ui.viewmodel

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.tranik.app.ui.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.tranik.app.data.model.LyricLine
import com.tranik.app.data.model.Track
import com.tranik.app.data.model.formatDuration
import com.tranik.app.data.repository.LyricsRepository
import com.tranik.app.data.repository.TrackRepository
import com.tranik.app.service.PlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val karaokeLines: List<LyricLine> = emptyList(),
    val activeKaraokeIndex: Int = -1,
    val miniPlayerVisible: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1
)

enum class RepeatMode { OFF, ALL, ONE }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val trackRepository: TrackRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var serviceContext: Context? = null
    private var lastNotifiedLyricIndex: Int = -1

    /**
     * تنظیم context برای Foreground Service
     */
    fun setServiceContext(context: Context) {
        serviceContext = context.applicationContext
    }

    /**
     * پخش یه ترک خاص + ساخت صف
     */
    fun playTrack(track: Track, context: Context, allTracks: List<Track> = emptyList()) {
        stopPlayback()
        serviceContext = context.applicationContext

        try {
            val mp = MediaPlayer()
            val uri = Uri.parse(track.contentUri)
            mp.setDataSource(context, uri)
            mp.prepare()
            mp.start()

            // سرعت پخش
            if (_state.value.playbackSpeed != 1.0f) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(_state.value.playbackSpeed)
                } catch (_: Exception) {}
            }

            mediaPlayer = mp

            // لود لیریک
            val lrcPath = lyricsRepository.findLrcForTrack(track.filePath)
            val karaokeLines = if (lrcPath != null) {
                lyricsRepository.readLrcFile(lrcPath)
            } else emptyList()

            // صف پخش
            val queueIndex = allTracks.indexOf(track).let { if (it >= 0) it else 0 }

            _state.update {
                it.copy(
                    currentTrack = track,
                    isPlaying = true,
                    progress = 0f,
                    currentPositionMs = 0,
                    totalDurationMs = mp.duration.toLong(),
                    karaokeLines = karaokeLines,
                    activeKaraokeIndex = -1,
                    miniPlayerVisible = false,
                    queue = allTracks,
                    queueIndex = queueIndex
                )
            }

            // شروع Foreground Service
            serviceContext?.let { ctx ->
                val isDark = _state.value.repeatMode != RepeatMode.OFF // placeholder — ideally from settings
                PlayerService.startWithTrack(ctx, track.displayName, track.displayArtist, track.albumId, true, true)
            }
            lastNotifiedLyricIndex = -1

            startProgressTracking()

            mp.setOnCompletionListener {
                onTrackCompleted()
            }
        } catch (e: Exception) {
            _state.update { it.copy(isPlaying = false) }
        }
    }

    /**
     * بعد از اتمام ترک
     */
    private fun onTrackCompleted() {
        val s = _state.value
        when (s.repeatMode) {
            RepeatMode.ONE -> {
                s.currentTrack?.let { _ ->
                    _state.update { it.copy(progress = 0f, currentPositionMs = 0) }
                    mediaPlayer?.let { mp ->
                        try {
                            mp.seekTo(0)
                            mp.start()
                            startProgressTracking()
                        } catch (_: Exception) {}
                    }
                }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (s.queueIndex < s.queue.size - 1) {
                    skipNext()
                } else {
                    _state.update {
                        it.copy(isPlaying = false, progress = 100f, miniPlayerVisible = false)
                    }
                    progressJob?.cancel()
                    serviceContext?.let { PlayerService.stop(it) }
                }
            }
        }
    }

    fun togglePlay() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
                progressJob?.cancel()
            } else {
                mp.start()
                _state.update { it.copy(isPlaying = true) }
                startProgressTracking()
            }
            // بروزرسانی نوتیفیکیشن
            serviceContext?.let { ctx ->
                PlayerService.updatePlayState(ctx, _state.value.isPlaying)
            }
        }
    }

    fun seekTo(percent: Float) {
        mediaPlayer?.let { mp ->
            val pos = (mp.duration * percent / 100f).toInt()
            mp.seekTo(pos)
            _state.update {
                it.copy(progress = percent, currentPositionMs = pos.toLong())
            }
            updateKaraokeIndex(pos.toLong())
        }
    }

    fun skipNext() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = if (s.shuffleEnabled) {
            (0 until s.queue.size).random()
        } else {
            (s.queueIndex + 1) % s.queue.size
        }
        _state.update { it.copy(queueIndex = nextIndex) }

        // پخش ترک بعدی
        val nextTrack = s.queue.getOrNull(nextIndex)
        if (nextTrack != null && serviceContext != null) {
            playTrack(nextTrack, serviceContext!!, s.queue)
        }
    }

    fun skipPrevious() {
        val s = _state.value
        if (s.queue.isEmpty()) return

        if (s.currentPositionMs > 3000) {
            mediaPlayer?.seekTo(0)
            _state.update { it.copy(progress = 0f, currentPositionMs = 0) }
        } else {
            val prevIndex = if (s.queueIndex > 0) s.queueIndex - 1 else s.queue.size - 1
            _state.update { it.copy(queueIndex = prevIndex) }

            val prevTrack = s.queue.getOrNull(prevIndex)
            if (prevTrack != null && serviceContext != null) {
                playTrack(prevTrack, serviceContext!!, s.queue)
            }
        }
    }

    fun toggleShuffle() {
        _state.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _state.update {
            it.copy(
                repeatMode = when (it.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        mediaPlayer?.let { mp ->
            try {
                mp.playbackParams = mp.playbackParams.setSpeed(speed)
            } catch (_: Exception) {}
        }
    }

    fun showMiniPlayer() {
        val s = _state.value
        if (s.currentTrack != null) {
            _state.update { it.copy(miniPlayerVisible = true) }
        }
    }

    fun hideMiniPlayer() {
        _state.update { it.copy(miniPlayerVisible = false) }
    }

    fun getQueuedTrack(): Track? {
        val s = _state.value
        if (s.queue.isEmpty() || s.queueIndex < 0 || s.queueIndex >= s.queue.size) return null
        return s.queue[s.queueIndex]
    }

    /**
     * گرفتن URI کاور آلبوم
     */
    fun getAlbumArtUri(albumId: Long): Uri {
        return trackRepository.getAlbumArtUri(albumId)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    try {
                        val current = mp.currentPosition.toLong()
                        val total = mp.duration.toLong()
                        val pct = if (total > 0) (current.toFloat() / total) * 100f else 0f

                        _state.update {
                            it.copy(progress = pct, currentPositionMs = current)
                        }
                        updateKaraokeIndex(current)
                    } catch (_: Exception) {}
                }
                delay(200)
            }
        }
    }

    private fun updateKaraokeIndex(currentMs: Long) {
        val lines = _state.value.karaokeLines
        if (lines.isEmpty()) return

        val index = lines.indexOfLast { it.timeMs in 0..currentMs }
        if (index != _state.value.activeKaraokeIndex) {
            _state.update { it.copy(activeKaraokeIndex = index) }

            // بروزرسانی لیریک در نوتیفیکیشن (فقط اگه خط عوض شده)
            if (index >= 0 && index != lastNotifiedLyricIndex) {
                lastNotifiedLyricIndex = index
                val line = lines[index]
                serviceContext?.let { ctx ->
                    PlayerService.updateLyric(ctx, line.text, line.translation ?: "")
                }
            }
        }
    }

    private fun stopPlayback() {
        progressJob?.cancel()
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        serviceContext?.let { PlayerService.stop(it) }
    }
}

package com.example.ipodnano.player

import android.app.Application
import android.content.ContentResolver
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val exoPlayer = ExoPlayer.Builder(application).build()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = exoPlayer.currentMediaItemIndex
                    _currentTrack.value = _tracks.value.getOrNull(index)
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                    }
                }
            }
        )

        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    fun loadTracks(contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val tracks = mutableListOf<Track>()

            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: ""
                    val artist = cursor.getString(artistColumn) ?: ""
                    val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        .buildUpon()
                        .appendPath(id.toString())
                        .build()
                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            uri = contentUri
                        )
                    )
                }
            }
            _tracks.value = tracks
        }
    }

    fun playTrack(track: Track) {
        val mediaItems = _tracks.value.map { MediaItem.fromUri(it.uri) }
        val startIndex = _tracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _currentTrack.value = track
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun skipNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        }
    }

    fun skipPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}

package com.malykbrown.musicwallpaper

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado partilhado da faixa actual.
 * O MediaNotificationListener escreve; o MusicWallpaperService observa.
 */
data class TrackInfo(
    val title: String = "",
    val artist: String = "",
    val art: Bitmap? = null,
    /** chave única para detectar mudança de arte sem comparar bitmaps */
    val artKey: String = ""
)

object NowPlaying {
    private val _state = MutableStateFlow(TrackInfo())
    val state: StateFlow<TrackInfo> = _state

    fun update(title: String, artist: String, art: Bitmap?) {
        val key = "$title|$artist|${art?.width}x${art?.height}"
        // Evita redesenhar se nada mudou
        if (key == _state.value.artKey) return
        _state.value = TrackInfo(title, artist, art, key)
    }
}

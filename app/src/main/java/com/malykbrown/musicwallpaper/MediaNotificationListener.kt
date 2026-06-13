package com.malykbrown.musicwallpaper

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/**
 * Lê as MediaSessions activas (Spotify, YouTube Music, Apple Music...)
 * e empurra título, artista e capa para o NowPlaying.
 *
 * Requer acesso às notificações (o utilizador activa no MainActivity).
 */
class MediaNotificationListener : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            pushMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Quando a reprodução muda, garante que temos a sessão certa
            refreshSessions()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(this, MediaNotificationListener::class.java)
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, component)
        } catch (_: SecurityException) {
            // Acesso às notificações ainda não concedido
        }
        refreshSessions()
    }

    override fun onListenerDisconnected() {
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        detachController()
        super.onListenerDisconnected()
    }

    private fun refreshSessions() {
        val component = ComponentName(this, MediaNotificationListener::class.java)
        val sessions = try {
            sessionManager?.getActiveSessions(component) ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        // Prefere a sessão que está a tocar; senão, a primeira disponível
        val playing = sessions.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: sessions.firstOrNull()

        if (playing?.sessionToken != activeController?.sessionToken) {
            detachController()
            activeController = playing?.also { it.registerCallback(controllerCallback) }
        }
        pushMetadata(activeController?.metadata)
    }

    private fun detachController() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun pushMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val art: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art != null) NowPlaying.update(title, artist, art)
    }
}

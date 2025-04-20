package com.example.mediaplayerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.core.net.toUri

class MediaPlayerService : Service() {

    companion object {
        var player: MediaPlayer? = null
        var currentUri: Uri? = null
        var playlist: List<Uri>? = null
        var index: Int = -1
        var currentFileName: String? = null

        const val ACTION_PLAY_FILE = "ACTION_PLAY_FILE"
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_FORWARD_10 = "ACTION_FORWARD_10"
        const val ACTION_REWIND_10 = "ACTION_REWIND_10"
        const val ACTION_STOP = "ACTION_STOP"

        private const val CHANNEL_ID = "MediaPlayerServiceChannel"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlaylist: List<Uri>? = null
    private var currentIndex: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        Log.d("MediaPlayerService", "Received intent with action: $action")

        if (action == ACTION_PLAY_FILE) {
            val uriString = intent.getStringExtra("uri") ?: return START_NOT_STICKY
            val uri = uriString.toUri()
            val playlistUris = intent.getStringArrayListExtra("playlist_uris") ?: arrayListOf(uriString)
            val uriList = playlistUris.map { it.toUri() }
            val index = intent.getIntExtra("index", 0)

            currentPlaylist = uriList
            currentIndex = index

            playlist = currentPlaylist
            MediaPlayerService.index = currentIndex

            playUri(uri)
        }

        when (action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> skipNext()
            ACTION_PREVIOUS -> skipPrevious()
            ACTION_FORWARD_10 -> seekBy(10_000)
            ACTION_REWIND_10 -> seekBy(-10_000)
            ACTION_STOP -> {
                stopSelf()
                PlayerStateBridge.viewModel?.updatePlayingState(false)
            }
        }

        return START_STICKY
    }

    private fun playUri(uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            prepare()
            start()
        }

        currentUri = uri
        player = mediaPlayer
        currentFileName = uri.lastPathSegment

        startForeground(1, buildNotification("Lecture en cours : $currentFileName"))
        PlayerStateBridge.viewModel?.updatePlayingState(true)
    }

    private fun resume() {
        mediaPlayer?.start()
        startForeground(1, buildNotification("Lecture en cours : $currentFileName"))
        PlayerStateBridge.viewModel?.updatePlayingState(true)
    }

    private fun pause() {
        Log.d("MediaPlayerService", "Trigger: PAUSE")
        mediaPlayer?.pause()
        startForeground(1, buildNotification("En pause : $currentFileName"))
        PlayerStateBridge.viewModel?.updatePlayingState(false)
    }

    private fun skipNext() {
        Log.d("MediaPlayerService", "Trigger: NEXT")
        if (!currentPlaylist.isNullOrEmpty() && currentIndex < currentPlaylist!!.size - 1) {
            currentIndex++
            val nextUri = currentPlaylist!![currentIndex]
            playUri(nextUri)

            currentUri = nextUri
            currentFileName = nextUri.lastPathSegment
            index = currentIndex
            PlayerStateBridge.viewModel?.updatePlayingState(true)

            val updateIntent = Intent("com.example.mediaplayerapp.UPDATE_UI")
            sendBroadcast(updateIntent)
        }
    }

    private fun skipPrevious() {
        Log.d("MediaPlayerService", "Trigger: PREVIOUS")
        if (!currentPlaylist.isNullOrEmpty() && currentIndex > 0) {
            currentIndex--
            val prevUri = currentPlaylist!![currentIndex]
            playUri(prevUri)

            currentUri = prevUri
            currentFileName = prevUri.lastPathSegment
            index = currentIndex
            PlayerStateBridge.viewModel?.updatePlayingState(true)

            val updateIntent = Intent("com.example.mediaplayerapp.UPDATE_UI")
            sendBroadcast(updateIntent)
        }
    }

    private fun seekBy(ms: Int) {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition + ms).coerceIn(0, it.duration)
            it.seekTo(newPosition)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val playIntent = Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PLAY }
        val pauseIntent = Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PAUSE }
        val nextIntent = Intent(this, MediaPlayerService::class.java).apply { action = ACTION_NEXT }
        val prevIntent = Intent(this, MediaPlayerService::class.java).apply { action = ACTION_PREVIOUS }
        val stopIntent = Intent(this, MediaPlayerService::class.java).apply { action = ACTION_STOP }

        val playPending = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevPending = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val isPlaying = mediaPlayer?.isPlaying == true
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Player")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(MediaStyle())
            .addAction(android.R.drawable.ic_media_previous, "Précédent", prevPending)
            .apply {
                if (isPlaying) {
                    addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
                } else {
                    addAction(android.R.drawable.ic_media_play, "Lecture", playPending)
                }
            }
            .addAction(android.R.drawable.ic_media_next, "Suivant", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Lecture audio", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

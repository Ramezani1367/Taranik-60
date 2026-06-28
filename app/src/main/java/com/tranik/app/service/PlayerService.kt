package com.tranik.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tranik.app.MainActivity
import com.tranik.app.R
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection

class PlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "taranik_player_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.tranik.app.PLAY"
        const val ACTION_PAUSE = "com.tranik.app.PAUSE"
        const val ACTION_NEXT = "com.tranik.app.NEXT"
        const val ACTION_PREVIOUS = "com.tranik.app.PREVIOUS"
        const val ACTION_STOP = "com.tranik.app.STOP"
        const val ACTION_UPDATE_TRACK = "com.tranik.app.UPDATE_TRACK"
        const val ACTION_UPDATE_LYRIC = "com.tranik.app.UPDATE_LYRIC"

        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM_ID = "album_id"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_LYRIC_LINE = "lyric_line"
        const val EXTRA_LYRIC_TRANSLATION = "lyric_translation"

        fun startWithTrack(
            context: Context,
            title: String,
            artist: String,
            albumId: Long,
            isPlaying: Boolean
        ) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_UPDATE_TRACK
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_ALBUM_ID, albumId)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateLyric(context: Context, lyricLine: String, translation: String?) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_UPDATE_LYRIC
                putExtra(EXTRA_LYRIC_LINE, lyricLine)
                putExtra(EXTRA_LYRIC_TRANSLATION, translation ?: "")
            }
            context.startService(intent)
        }

        fun updatePlayState(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = if (isPlaying) ACTION_PLAY else ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentTitle = "ترانیک"
    private var currentArtist = ""
    private var currentAlbumId: Long = -1
    private var isCurrentlyPlaying = true
    private var currentLyricLine = ""
    private var currentLyricTranslation = ""
    private var currentCoverBitmap: Bitmap? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onPreviousClicked: (() -> Unit)? = null
    var onNextClicked: (() -> Unit)? = null
    var onPlayPauseClicked: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_TRACK -> {
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: "ترانیک"
                currentArtist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                currentAlbumId = intent.getLongExtra(EXTRA_ALBUM_ID, -1)
                isCurrentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                currentLyricLine = ""
                currentLyricTranslation = ""
                loadCoverAndShowNotification()
            }

            ACTION_UPDATE_LYRIC -> {
                currentLyricLine = intent.getStringExtra(EXTRA_LYRIC_LINE) ?: ""
                currentLyricTranslation = intent.getStringExtra(EXTRA_LYRIC_TRANSLATION) ?: ""
                updateNotification()
            }

            ACTION_PLAY -> {
                isCurrentlyPlaying = true
                onPlayPauseClicked?.invoke()
                updateNotification()
            }

            ACTION_PAUSE -> {
                isCurrentlyPlaying = false
                onPlayPauseClicked?.invoke()
                updateNotification()
            }

            ACTION_NEXT -> {
                onNextClicked?.invoke()
            }

            ACTION_PREVIOUS -> {
                onPreviousClicked?.invoke()
            }

            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * لود کردن کاور آلبوم از MediaStore و نمایش نوتیفیکیشن
     */
    private fun loadCoverAndShowNotification() {
        // اول نوتیفیکیشن رو بدون کاور نشون بده (سریع)
        showNotification()

        // بعد کاور رو async لود کن
        if (currentAlbumId >= 0) {
            serviceScope.launch {
                try {
                    val bitmap = loadAlbumArtBitmap(currentAlbumId)
                    currentCoverBitmap = bitmap
                    // بروزرسانی نوتیفیکیشن با کاور
                    withContext(Dispatchers.Main) {
                        updateNotification()
                    }
                } catch (e: Exception) {
                    // کاور پیدا نشد — بدون کاور بمونه
                    currentCoverBitmap = null
                }
            }
        } else {
            currentCoverBitmap = null
        }
    }

    /**
     * خواندن Bitmap کاور آلبوم از ContentResolver
     */
    private suspend fun loadAlbumArtBitmap(albumId: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val uri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                val inputStream: InputStream? = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    // اول سایز رو ببین — نباید از 512px بزرگتر باشه (Binder limit)
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    // محاسبه sampleSize برای downscale
                    val targetSize = 256 // 256x256 برای نوتیفیکیشن کافیه
                    val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetSize)

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }

                    val secondStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                    secondStream?.close()

                    bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        if (width > targetSize || height > targetSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= targetSize && halfHeight / sampleSize >= targetSize) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * ساخت placeholder bitmap وقتی کاور وجود نداره
     */
    private fun createPlaceholderBitmap(): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // پس‌زمینه تیره
        canvas.drawColor(Color.parseColor("#1A1A24"))

        // آیکون موسیقی ساده
        val paint = Paint().apply {
            color = Color.parseColor("#7C6FE0")
            alpha = 150
            isAntiAlias = true
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♪", size / 2f, size / 2f + 28f, paint)

        return bitmap
    }

    private fun showNotification() {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        try {
            val notification = buildNotification()
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // نوتیفیکیشن ممکنه هنوز ساخته نشده باشه
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPi = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, PlayerService::class.java).apply {
            action = if (isCurrentlyPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePi = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_NEXT }
        val nextPi = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Custom layout
        val customView = RemoteViews(packageName, R.layout.notification_player)

        // Track info
        customView.setTextViewText(R.id.tv_title, currentTitle)
        customView.setTextViewText(R.id.tv_artist, currentArtist)

        // ✅ Cover Art — Bitmap لود شده یا placeholder
        val coverBitmap = currentCoverBitmap ?: createPlaceholderBitmap()
        customView.setImageViewBitmap(R.id.iv_cover, coverBitmap)

        // Lyrics
        if (currentLyricLine.isNotBlank()) {
            customView.setTextViewText(R.id.tv_lyric_line, currentLyricLine)
            customView.setTextViewText(R.id.tv_lyric_translation, currentLyricTranslation)
            customView.setViewVisibility(R.id.lyrics_container, android.view.View.VISIBLE)
        } else {
            customView.setTextViewText(R.id.tv_lyric_line, "")
            customView.setTextViewText(R.id.tv_lyric_translation, "")
            customView.setViewVisibility(R.id.lyrics_container, android.view.View.GONE)
        }

        // Play/Pause button
        customView.setImageViewResource(
            R.id.btn_play_pause,
            if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        // Click handlers
        customView.setOnClickPendingIntent(R.id.btn_prev, prevPi)
        customView.setOnClickPendingIntent(R.id.btn_play_pause, playPausePi)
        customView.setOnClickPendingIntent(R.id.btn_next, nextPi)
        customView.setOnClickPendingIntent(R.id.btn_close, stopPi)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setContentIntent(openPi)
            .setDeleteIntent(stopPi)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "پخش موسیقی ترانیک",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "کنترل پخش موسیقی با نمایش لیریک و کاور"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        currentCoverBitmap?.recycle()
        currentCoverBitmap = null
    }
}

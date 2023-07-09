package com.example.music

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.music.activities.MusicInterface
import com.example.music.utils.ApplicationClass
import com.example.music.utils.NowPlaying

class MusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable
    lateinit var audioManager: AudioManager
    val broadcastReceiver: MyBroadcastReceiver = MyBroadcastReceiver()
    companion object {
        lateinit var playPendingIntent: PendingIntent
    }
    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "Music")
        return myBinder
    }
    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(playPauseButton: Int) {

        val intent = Intent(baseContext, MusicInterface::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("index", MusicInterface.songPosition)
        intent.putExtra("class", "Now Playing Notification")

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentIntent = PendingIntent.getActivity(this, 0, intent, flag)

        val prevIntent =
            Intent(baseContext, MyBroadcastReceiver::class.java).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(
            baseContext, 3, prevIntent, flag

        )

        val playIntent =
            Intent(baseContext, MyBroadcastReceiver::class.java).setAction(ApplicationClass.PLAY)
        playPendingIntent = PendingIntent.getBroadcast(
            baseContext, 3, playIntent, flag

        )

        val nextIntent =
            Intent(baseContext, MyBroadcastReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(
            baseContext, 3, nextIntent, flag

        )

        val exitIntent =
            Intent(baseContext, MyBroadcastReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(
            baseContext, 3, exitIntent, flag

        )


        val imageArt = getImageArt(MusicInterface.musicList[MusicInterface.songPosition].path)
        val image = if (imageArt != null) {
            BitmapFactory.decodeByteArray(imageArt, 0, imageArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.image_as_cover)
        }

        val notification = NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
            .setContentTitle(MusicInterface.musicList[MusicInterface.songPosition].title)
            .setContentText(MusicInterface.musicList[MusicInterface.songPosition].artist)
            .setSubText(MusicInterface.musicList[MusicInterface.songPosition].album)
            .setSmallIcon(R.drawable.music_note)
            .setLargeIcon(image)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            ).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(true)
            .addAction(R.drawable.navigate_before_notification, "Previous", prevPendingIntent)
            .addAction(playPauseButton, "PlayPause", playPendingIntent)
            .addAction(R.drawable.navigate_next_notification, "Next", nextPendingIntent)
            .addAction(R.drawable.close_notification, "Exit", exitPendingIntent).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val playbackSpeed = if (MusicInterface.isPlaying) 1F else 0F
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer!!.duration.toLong()
                    )
                    .build()
            )
            val playBackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    mediaPlayer!!.currentPosition.toLong(),
                    playbackSpeed
                )
                .setActions(
                    PlaybackStateCompat.ACTION_SEEK_TO
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .build()
            mediaSession.setPlaybackState(playBackState)
            mediaSession.isActive = true
            mediaSession.setCallback(object : MediaSessionCompat.Callback() {

                override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                    val event =
                        mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (event != null) {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (event.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    if (mediaPlayer != null) {
                                        if (MusicInterface.isPlaying) {
                                            //pause music
                                            MusicInterface.binding.interfacePlay.setImageResource(R.drawable.play)
                                            MusicInterface.isPlaying = false
                                            mediaPlayer!!.pause()
                                            showNotification(R.drawable.play_notification)
                                            NowPlaying.binding.fragmentButton.setImageResource(R.drawable.play_now)
                                        } else {
                                            //play music
                                            MusicInterface.binding.interfacePlay.setImageResource(
                                                R.drawable.pause
                                            )
                                            MusicInterface.isPlaying = true
                                            mediaPlayer!!.start()
                                            showNotification(R.drawable.pause_notification)
                                            NowPlaying.binding.fragmentButton.setImageResource(
                                                R.drawable.pause_now
                                            )

                                        }
                                    }
                                }
                                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                    broadcastReceiver.prevNextMusic(increment = true, baseContext)
                                    MusicInterface.counter--
                                }
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                    broadcastReceiver.prevNextMusic(increment = false, baseContext)
                                }
                            }
                            return true
                        }
                    }
                    return false
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer!!.seekTo(pos.toInt())
                    val playBackStateNew = PlaybackStateCompat.Builder()
                        .setState(
                            PlaybackStateCompat.STATE_PLAYING,
                            mediaPlayer!!.currentPosition.toLong(),
                            playbackSpeed
                        )
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build()
                    mediaSession.setPlaybackState(playBackStateNew)
                }
            })
        }
        startForeground(3, notification)
    }

    fun initSong() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            MusicInterface.musicService!!.mediaPlayer!!.reset()
            MusicInterface.musicService!!.mediaPlayer!!.setDataSource(MusicInterface.musicList[MusicInterface.songPosition].path)
            MusicInterface.musicService!!.mediaPlayer!!.prepare()
            MusicInterface.musicService!!.showNotification(R.drawable.pause_notification)
            MusicInterface.binding.interfacePlay.setImageResource((R.drawable.pause))
            MusicInterface.binding.interfaceSeekStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong())
            MusicInterface.binding.interfaceSeekEnd.text =
                formatDuration(mediaPlayer!!.duration.toLong())
            MusicInterface.binding.seekbar.progress = 0
            MusicInterface.binding.seekbar.max =
                mediaPlayer!!.duration

        } catch (e: Exception) {
            return
        }
    }

    fun seekBarHandler() {
        runnable = Runnable {
            MusicInterface.binding.interfaceSeekStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong())
            MusicInterface.binding.seekbar.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)

    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) {
            //pause music
            MusicInterface.binding.interfacePlay.setImageResource(R.drawable.play)
            MusicInterface.isPlaying = false
            NowPlaying.binding.fragmentButton.setImageResource(R.drawable.play_now)
            mediaPlayer!!.pause()
            showNotification(R.drawable.play_notification)

        } else {
            //play music
            MusicInterface.binding.interfacePlay.setImageResource(R.drawable.pause)
            MusicInterface.isPlaying = true
            mediaPlayer!!.start()
            NowPlaying.binding.fragmentButton.setImageResource(R.drawable.pause_now)
            showNotification(R.drawable.pause_notification)
        }

    }

}

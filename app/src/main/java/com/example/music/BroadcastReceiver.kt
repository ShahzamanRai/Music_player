package com.example.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class BroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ApplicationClass.PREVIOUS -> {
                prevNextMusic(increment = false, context = context!!)
            }
            ApplicationClass.PLAY -> {
                if (MusicInterface.isPlaying) pauseMusic() else playMusic()
            }
            ApplicationClass.NEXT -> {
                prevNextMusic(increment = true, context = context!!)
            }
            ApplicationClass.EXIT -> {
                exitApplication()
            }
        }
    }

    private fun playMusic() {
        MusicInterface.isPlaying = true
        MusicInterface.binding.interfacePlay.setImageResource(R.drawable.pause)
        MusicInterface.musicService!!.mediaPlayer!!.start()
        MusicInterface.musicService!!.showNotification(R.drawable.pause_notification)
        MusicInterface.binding.interfaceSeekStart.text =
            formatDuration(MusicInterface.musicService!!.mediaPlayer!!.currentPosition.toLong())
        MusicInterface.binding.interfaceSeekEnd.text =
            formatDuration(MusicInterface.musicService!!.mediaPlayer!!.duration.toLong())
        MusicInterface.binding.seekbar.progress = 0
        MusicInterface.binding.seekbar.max = MusicInterface.musicService!!.mediaPlayer!!.duration

    }

    private fun prevNextMusic(increment: Boolean, context: Context) {
        setSongPosition(increment = increment)
        MusicInterface.musicService!!.initSong()
        Glide.with(context).load(MusicInterface.musicList[MusicInterface.songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
            ).into(MusicInterface.binding.interfaceCover)

        MusicInterface.binding.interfaceSongName.text =
            MusicInterface.musicList[MusicInterface.songPosition].title
        MusicInterface.binding.interfaceArtistName.text =
            MusicInterface.musicList[MusicInterface.songPosition].album
        playMusic()
    }

    private fun pauseMusic() {
        MusicInterface.isPlaying = false
        MusicInterface.binding.interfacePlay.setImageResource(R.drawable.play)
        MusicInterface.musicService!!.mediaPlayer!!.pause()
        MusicInterface.musicService!!.showNotification(R.drawable.play_notification)

    }
}
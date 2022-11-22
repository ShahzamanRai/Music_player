package com.example.music

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class MusicClass(
    val id: String,
    val title: String,
    val album: String,
    val length: Long = 0,
    val artist: String,
    val path: String,
    val artUri: String
)

fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(
        duration, TimeUnit.MILLISECONDS
    ) - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d", minutes, seconds)
}

fun getImageArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}

fun exitApplication() {
    if (MusicInterface.musicService != null) {
        MusicInterface.musicService!!.audioManager.abandonAudioFocus(MusicInterface.musicService)
        MusicInterface.musicService!!.stopForeground(true)
        MusicInterface.musicService!!.mediaPlayer!!.release()
        MusicInterface.musicService = null
    }
    exitProcess(1)
}

fun setSongPosition(increment: Boolean) {
    if (!MusicInterface.isRepeating) {
        if (!MusicInterface.isShuffling) {
            if (increment) {
                if (MusicInterface.musicList.size - 1 == MusicInterface.songPosition) {
                    MusicInterface.songPosition = 0
                } else ++MusicInterface.songPosition
            } else {
                if (0 == MusicInterface.songPosition) MusicInterface.songPosition =
                    MusicInterface.musicList.size - 1
                else --MusicInterface.songPosition
            }
        } else {
            shuffleSongs()
        }
    }
}

fun shuffleSongs() {
    var newSong: Int = MusicInterface.songPosition
    while (newSong == MusicInterface.songPosition) {
        newSong = kotlin.random.Random.nextInt(MusicInterface.musicList.size);
    }
    MusicInterface.songPosition = newSong;
}

fun favouriteCheck(id: String): Int {
    MusicInterface.isLiked = false
    FavouriteActivity.favSongList.forEachIndexed { index, music ->
        if (id == music.id) {
            MusicInterface.isLiked = true
            return index
        }
    }
    return -1
}


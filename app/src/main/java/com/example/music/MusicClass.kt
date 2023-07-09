package com.example.music

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.music.activities.FavouriteActivity
import com.example.music.activities.MusicInterface
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.random.Random
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

class Playlist {
    lateinit var name: String
    lateinit var playlist: ArrayList<MusicClass>
    lateinit var createdBy: String
    lateinit var createdOn: String
}

class MusicPlaylist {
    var ref: ArrayList<Playlist> = ArrayList()
}

val usedNumber = mutableSetOf<Int>()

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

fun exitApplicationNotification() {
    if (MusicInterface.isPlaying) {
        val musicInterface = MusicInterface()
        musicInterface.pauseMusic()
    }
    MusicInterface.musicService!!.stopForeground(true)
}

fun checkPlaylist(playlist: ArrayList<MusicClass>): ArrayList<MusicClass> {
    playlist.forEachIndexed { index, music ->
        val file = File(music.path)
        if (!file.exists())
            playlist.removeAt(index)
    }
    return playlist
}

fun setSongPosition(increment: Boolean) {
    if (!MusicInterface.isRepeating) {
        if (increment) {
            if (MusicInterface.isShuffling && MusicInterface.counter == 0) {
                shuffleSongs()
            } else {
                if (MusicInterface.musicList.size - 1 == MusicInterface.songPosition) {
                    MusicInterface.songPosition = 0
                } else ++MusicInterface.songPosition
            }
        } else {
            if (0 == MusicInterface.songPosition) MusicInterface.songPosition =
                MusicInterface.musicList.size - 1
            else --MusicInterface.songPosition
        }
    }
}

fun checkIfListIsFull(list: MutableSet<Int>) {
    if (list.size.toInt() == MusicInterface.musicList.size) {
        list.clear()
    }
}

fun shuffleSongs() {
    var newSong: Int = MusicInterface.songPosition
    checkIfListIsFull(usedNumber)
    Log.d(TAG, "shuffleSongs: " + usedNumber.size)
    while (newSong == MusicInterface.songPosition) {
        newSong = getRandomNumber(MusicInterface.musicList.size)
    }
    MusicInterface.songPosition = newSong
}

fun getRandomNumber(max: Int): Int {
    val random = Random
    var number = random.nextInt(max + 1)

    while (usedNumber.contains(number)) {
        number = random.nextInt(max + 1)
    }

    usedNumber.add(number)
    return number
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

fun getMainColor(img: Bitmap): Int {
    val newImg = Bitmap.createScaledBitmap(img, 1, 1, true)
    val color = newImg.getPixel(0, 0)
    newImg.recycle()
    return manipulateColor(color, 0.4.toFloat())
}


fun manipulateColor(color: Int, factor: Float): Int {
    val a: Int = Color.alpha(color)
    val r = (Color.red(color) * factor).roundToInt()
    val g = (Color.green(color) * factor).roundToInt()
    val b = (Color.blue(color) * factor).roundToInt()
    return Color.argb(
        a,
        r.coerceAtMost(255),
        g.coerceAtMost(255),
        b.coerceAtMost(255)
    )
}





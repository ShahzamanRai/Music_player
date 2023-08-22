package com.example.music.activities

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.music.MusicClass
import com.example.music.MusicService
import com.example.music.R
import com.example.music.databinding.ActivityMusicInterfaceBinding
import com.example.music.exitApplication
import com.example.music.favouriteCheck
import com.example.music.formatDuration
import com.example.music.getImageArt
import com.example.music.getMainColor
import com.example.music.setSongPosition
import com.example.music.utils.NowPlaying
import com.example.music.utils.OnSwipeTouchListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MusicInterface : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {
    private lateinit var afChangeListener: AudioManager.OnAudioFocusChangeListener

    companion object {
        lateinit var musicList: ArrayList<MusicClass>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var isRepeating: Boolean = false
        var isShuffling: Boolean = false
        var counter: Int = 0
            set(value) {
                field = kotlin.math.max(value, 0)
            }
        var fIndex: Int = -1
        var isLiked: Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMusicInterfaceBinding
        var musicService: MusicService? = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicInterfaceBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)
        binding.interfaceSongName.isSelected = true
        if (intent.data?.scheme.contentEquals("content")) {
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicList = ArrayList()
            musicList.add(getMusicDetails(intent.data!!))
            Glide.with(this).load(getImageArt(musicList[songPosition].path)).apply(
                RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
            ).into(binding.interfaceCover)
            binding.interfaceSongName.text =
                musicList[songPosition].title.removePrefix("/storage/emulated/0/")
            binding.interfaceArtistName.text = musicList[songPosition].album
        } else {
            initActivity()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.interfaceNext.setOnClickListener {
            prevNextSong(increment = true)
        }
        binding.interfacePrevious.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.interfaceTimer.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer").setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes") { _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.interfaceTimer.setColorFilter(
                            ContextCompat.getColor(
                                this, R.color.bgTimer
                            )
                        )
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
            }
        }

        binding.interfaceShare.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))

        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, isUser: Boolean) {
                try {

                    if (isUser) {
                        musicService!!.mediaPlayer!!.seekTo(progress)
                        musicService!!.showNotification(if (isPlaying) R.drawable.pause_notification else R.drawable.play_notification)
                    }
                } catch (e: Exception) {
                    return
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) = Unit

            override fun onStopTrackingTouch(p0: SeekBar?) = Unit
        })

        binding.interfacePlay.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()

        }

        binding.interfaceLikeButton.setOnClickListener {
            fIndex = favouriteCheck(musicList[songPosition].id)
            if (isLiked) {
                isLiked = false
                binding.interfaceLikeButton.setImageResource(R.drawable.heart)
                NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart_fragment)
                FavouriteActivity.favSongList.removeAt(fIndex)
            } else {
                isLiked = true
                binding.interfaceLikeButton.setImageResource(R.drawable.heart_fill)
                NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart_fill)
                FavouriteActivity.favSongList.add(musicList[songPosition])
            }

        }
        binding.interfaceEqualizer.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId
                )
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 3)
            } catch (e: Exception) {
                Snackbar.make(
                    this, it, "Equalizer feature not supported in your device.", 3000
                ).show()

            }
        }

        binding.interfaceCover.setOnTouchListener(object : OnSwipeTouchListener(baseContext) {
            override fun onSingleClick() {
                if (isPlaying) {
                    pauseMusic()
                } else {
                    playMusic()
                }
            }

            override fun onSwipeDown() {
                Log.d(ContentValues.TAG, "onSwipeDown: Performed")
                finish()
            }

            override fun onSwipeLeft() {
                prevNextSong(increment = true)
            }

            override fun onSwipeRight() {
                prevNextSong(increment = false)
            }
        })

        binding.root.setOnTouchListener(object : OnSwipeTouchListener(baseContext) {

            override fun onSwipeDown() {
                Log.d(ContentValues.TAG, "onSwipeDown: Performed")
                finish()
            }

            override fun onSwipeLeft() {
                prevNextSong(increment = true)
            }

            override fun onSwipeRight() {
                prevNextSong(increment = false)
            }
        })

        binding.interfaceRepeat.setOnClickListener {
            if (!isRepeating) {
                isRepeating = true
                binding.interfaceRepeat.setImageResource(R.drawable.repeat_on)
                binding.interfaceRepeat.setColorFilter(ContextCompat.getColor(this, R.color.green))
            } else {
                isRepeating = false
                binding.interfaceRepeat.setImageResource(R.drawable.repeat)
                binding.interfaceRepeat.setColorFilter(
                    ContextCompat.getColor(
                        this, R.color.music_icon_tint
                    )
                )
            }
        }

        binding.interfaceShuffle.setOnClickListener {
            if (!isShuffling) {
                isShuffling = true
                binding.interfaceShuffle.setImageResource(R.drawable.shuffle_fill)
                binding.interfaceShuffle.setColorFilter(ContextCompat.getColor(this, R.color.green))

            } else {
                isShuffling = false
                binding.interfaceShuffle.setImageResource(R.drawable.shuffle)
                binding.interfaceShuffle.setColorFilter(
                    ContextCompat.getColor(
                        this, R.color.music_icon_tint
                    )
                )
            }
        }
    }

    private fun initActivity() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "MusicAdapter" -> {
                initServiceAndPlaylist(MainActivity.songList, shuffle = false)
            }

            "FavAdapterSearch" -> {
                initServiceAndPlaylist(FavouriteActivity.musicListSearch, shuffle = false)
            }

            "FavAdapter" -> {
                initServiceAndPlaylist(FavouriteActivity.favSongList, shuffle = false)
            }

            "FavouriteShuffle" -> {
                initServiceAndPlaylist(FavouriteActivity.favSongList, shuffle = true)
            }

            "PlaylistDetailsAdapter" -> {
                initServiceAndPlaylist(
                    PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist,
                    shuffle = false
                )
            }

            "PlaylistDetailsShuffle" -> {
                initServiceAndPlaylist(
                    PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist,
                    shuffle = true
                )
            }

            "Now playing" -> {
                showMusicInterfacePlaying()
            }

            "Now Playing Notification" -> {
                showMusicInterfacePlaying()
            }

            "MusicAdapterSearch" -> {
                initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            }
        }
    }

    private fun setLayout() {
        try {
            fIndex = favouriteCheck(musicList[songPosition].id)
            Glide.with(this).load(getImageArt(musicList[songPosition].path)).apply(
                RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
            ).into(binding.interfaceCover)

            binding.interfaceSongName.text = musicList[songPosition].title
            binding.interfaceArtistName.text = musicList[songPosition].album
            if (isRepeating) {
                binding.interfaceRepeat.setImageResource(R.drawable.repeat_on)
                binding.interfaceRepeat.setColorFilter(ContextCompat.getColor(this, R.color.green))
            }
            if (isShuffling) {
                binding.interfaceShuffle.setColorFilter(ContextCompat.getColor(this, R.color.green))
                binding.interfaceShuffle.setImageResource(R.drawable.shuffle_fill)
            }
            if (isLiked) {
                NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart_fill)
                binding.interfaceLikeButton.setImageResource(R.drawable.heart_fill)
            } else {
                NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart)
                binding.interfaceLikeButton.setImageResource(R.drawable.heart)
            }

            val img = getImageArt(musicList[songPosition].path)
            val image = if (img != null) {
                BitmapFactory.decodeByteArray(img, 0, img.size)
            } else {
                BitmapFactory.decodeResource(
                    resources, R.drawable.image_as_cover
                )
            }
            val bgColor = getMainColor(image)
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xFFFFFF, bgColor)
            )
            binding.root.background = gradient
            window?.statusBarColor = bgColor
        } catch (e: Exception) {
            return
        }
    }

    private fun initSong() {
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicList[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            binding.interfacePlay.setImageResource((R.drawable.pause))
            binding.interfaceSeekStart.text =
                formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.interfaceSeekEnd.text =
                formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekbar.progress = 0
            binding.seekbar.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            playMusic()

        } catch (e: Exception) {
            return
        }

    }

    private fun playMusic() {
        try {
            musicService!!.audioManager.requestAudioFocus(
                musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
            isPlaying = true
            musicService!!.mediaPlayer!!.start()
            binding.interfacePlay.setImageResource((R.drawable.pause))
            musicService!!.showNotification(R.drawable.pause_notification)
            NowPlaying.binding.fragmentButton.setImageResource(R.drawable.pause_now)
        } catch (e: Exception) {
            return
        }
    }

    fun pauseMusic() {
        try {
            musicService!!.audioManager.abandonAudioFocus(musicService)
            isPlaying = false
            musicService!!.mediaPlayer!!.pause()
            binding.interfacePlay.setImageResource((R.drawable.play))
            musicService!!.showNotification(R.drawable.play_notification)
            NowPlaying.binding.fragmentButton.setImageResource(R.drawable.play_now)
        } catch (e: Exception) {
            return
        }
    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            initSong()
            counter--
        } else {
            setSongPosition(increment = false)
            setLayout()
            initSong()
        }

    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (musicService == null) {
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(
                musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        initSong()
        musicService!!.seekBarHandler()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(p0: MediaPlayer?) {
        setSongPosition(increment = true)
        setLayout()
        initSong()
        counter--

        //for refreshing now playing image & text on song completion
        NowPlaying.binding.fragmentTitle.isSelected = true
        Glide.with(applicationContext).load(getImageArt(musicList[songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop())
            .into(NowPlaying.binding.fragmentImage)
        NowPlaying.binding.fragmentTitle.text = musicList[songPosition].title
        NowPlaying.binding.fragmentAlbumName.text = musicList[songPosition].title

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 || resultCode == RESULT_OK) return


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMusicDetails(contentUri: Uri): MusicClass {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            return MusicClass(
                id = "Unknown",
                title = path.toString(),
                album = "Unknown",
                artist = "Unknown",
                length = duration,
                artUri = "Unknown",
                path = path.toString()
            )
        } finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (musicList[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }

    private fun showMusicInterfacePlaying() {
        setLayout()
        binding.interfaceSeekStart.text =
            formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
        binding.interfaceSeekEnd.text =
            formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
        binding.seekbar.progress = musicService!!.mediaPlayer!!.currentPosition
        binding.seekbar.max = musicService!!.mediaPlayer!!.duration
        if (isPlaying) {
            binding.interfacePlay.setImageResource((R.drawable.pause))
        } else {
            binding.interfacePlay.setImageResource((R.drawable.play))
        }
        if (isLiked) {
            NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart_fill)
        } else {
            NowPlaying.binding.fragmentHeartButton.setImageResource(R.drawable.heart)
        }
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@MusicInterface)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 15 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.interfaceTimer.setColorFilter(ContextCompat.getColor(this, R.color.green))
            min15 = true
            Thread {
                Thread.sleep((15 * 60000).toLong())
                if (min15) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.interfaceTimer.setColorFilter(ContextCompat.getColor(this, R.color.green))
            min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 60 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.interfaceTimer.setColorFilter(ContextCompat.getColor(this, R.color.green))
            min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (min60) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, com.google.android.material.R.anim.mtrl_bottom_sheet_slide_out)
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(com.google.android.material.R.anim.mtrl_bottom_sheet_slide_in, 0)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            showMusicInterfacePlaying()
        }
    }

    fun initServiceAndPlaylist(
        playlist: ArrayList<MusicClass>, shuffle: Boolean
    ) {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicList = ArrayList()
        musicList.addAll(playlist)
        if (shuffle) musicList.shuffle()
        setLayout()
    }
}

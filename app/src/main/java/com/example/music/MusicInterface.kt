package com.example.music

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.music.databinding.ActivityMusicInterfaceBinding


class MusicInterface : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicList: ArrayList<MusicClass>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var isRepeating: Boolean = false
        var isShuffling: Boolean = false
        var fIndex: Int = -1
        var isLiked: Boolean = false

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMusicInterfaceBinding
        var musicService: MusicService? = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicInterfaceBinding.inflate(layoutInflater)
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
                val toast = Toast.makeText(
                    this, "Equalizer feature not supported in your device", Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }

        binding.interfaceCover.setOnTouchListener(object : OnSwipeTouchListener(baseContext) {

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
            } else {
                isRepeating = false
                binding.interfaceRepeat.setImageResource(R.drawable.repeat)
            }
        }

        binding.interfaceShuffle.setOnClickListener {
            if (!isShuffling) {
                isShuffling = true
                binding.interfaceShuffle.setImageResource(R.drawable.shuffle_fill)
            } else {
                isShuffling = false
                binding.interfaceShuffle.setImageResource(R.drawable.shuffle)
            }
        }


    }

    private fun initActivity() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "MusicAdapter" -> {
                startService()
                musicList = ArrayList()
                musicList.addAll(MainActivity.songList)
                setLayout()
                initSong()
            }
            "Now playing" -> {
                showMusicInterfacePlaying()
            }
            "Now Playing Notification" -> {
                showMusicInterfacePlaying()
            }

            "MusicAdapterSearch" -> {
                startService()
                musicList = ArrayList()
                musicList.addAll(MainActivity.filteredList)
                setLayout()
                initSong()
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
            if (isRepeating) binding.interfaceRepeat.setImageResource(R.drawable.repeat_on)
            if (isShuffling) binding.interfaceShuffle.setImageResource(R.drawable.shuffle_fill)
            if (isLiked) binding.interfaceLikeButton.setImageResource(R.drawable.heart)
            else binding.interfaceLikeButton.setImageResource(R.drawable.heart)
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
            isPlaying = true
            musicService!!.mediaPlayer!!.start()
            binding.interfacePlay.setImageResource((R.drawable.pause))
            musicService!!.showNotification(R.drawable.pause_notification)
        } catch (e: Exception) {
            return
        }
    }

    private fun pauseMusic() {
        try {
            isPlaying = false
            musicService!!.mediaPlayer!!.pause()
            binding.interfacePlay.setImageResource((R.drawable.play))
            musicService!!.showNotification(R.drawable.play_notification)
        } catch (e: Exception) {
            return
        }
    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            initSong()
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
        initSong()
        setLayout()

        //for refreshing now playing image & text on song completion
        NowPlaying.binding.fragmentTitle.isSelected = true
        Glide.with(applicationContext)
            .load(getImageArt(MusicInterface.musicList[MusicInterface.songPosition].path))
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

    private fun startService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
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
            binding.interfaceLikeButton.setImageResource(R.drawable.heart_fill)
        } else {
            binding.interfaceLikeButton.setImageResource(R.drawable.heart)
        }
    }
}
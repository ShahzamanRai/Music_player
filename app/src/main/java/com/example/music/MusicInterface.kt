package com.example.music

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
        var isrepeating: Boolean = false

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMusicInterfaceBinding
        var musicService: MusicService? = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicInterfaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity()
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
                if (isUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if(isPlaying) R.drawable.pause_notification else R.drawable.play_notification)
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
            binding.interfaceLikeButton.setImageResource(R.drawable.heart_fill)
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
                toast.getView()?.setBackgroundColor(getColor(R.color.blue));
                toast.show()
            }
        }
        binding.interfaceRepeat.setOnClickListener {
            if (!isrepeating) {
                isrepeating = true
                binding.interfaceRepeat.setImageResource(R.drawable.repeat_on)
            } else {
                isrepeating = false
                binding.interfaceRepeat.setImageResource(R.drawable.repeat)
            }
        }
    }

    private fun initActivity() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "MusicAdapter" -> {
                musicList = ArrayList()
                musicList.addAll(MainActivity.songList)
                setLayout()
            }

            "MusicAdapterSearch" -> {
                musicList = ArrayList()
                musicList.addAll(MainActivity.filteredList)
                setLayout()
            }
        }
        if (musicService != null && !isPlaying) playMusic()
    }

    private fun setLayout() {

        Glide.with(this).load(musicList[songPosition].artUri).apply(
            RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
        ).into(binding.interfaceCover)

        binding.interfaceSongName.text = musicList[songPosition].title
        binding.interfaceArtistName.text = musicList[songPosition].album
        if (isrepeating) binding.interfaceRepeat.setImageResource(R.drawable.repeat_on)

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
            musicService!!.showNotification(R.drawable.play_notification)
            binding.interfacePlay.setImageResource((R.drawable.play))
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
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(
                musicService,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
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

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 || resultCode == RESULT_OK) return


    }

    override fun onDestroy() {
        super.onDestroy()
        if (musicList[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }
}
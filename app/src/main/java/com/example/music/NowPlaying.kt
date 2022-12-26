package com.example.music

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.music.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.GONE

        binding.fragmentHeartButton.setOnClickListener {
            MusicInterface.fIndex =
                favouriteCheck(MusicInterface.musicList[MusicInterface.songPosition].id)
            if (MusicInterface.isLiked) {
                MusicInterface.isLiked = false
                MusicInterface.binding.interfaceLikeButton.setImageResource(R.drawable.heart)
                binding.fragmentHeartButton.setImageResource(R.drawable.heart_fragment)
                FavouriteActivity.favSongList.removeAt(MusicInterface.fIndex)
            } else {
                MusicInterface.isLiked = true
                MusicInterface.binding.interfaceLikeButton.setImageResource(R.drawable.heart_fill)
                binding.fragmentHeartButton.setImageResource(R.drawable.heart_fill)
                FavouriteActivity.favSongList.add(MusicInterface.musicList[MusicInterface.songPosition])
            }
        }

        binding.fragmentButton.setOnClickListener {
            if (MusicInterface.isPlaying) pauseMusic()
            else playMusic()
        }
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        if (MusicInterface.musicService != null) {
            binding.root.visibility = View.VISIBLE
            binding.fragmentTitle.isSelected = true

            binding.root.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
                override fun onSingleClick() {
                    openActivity()
                }

                override fun onSwipeTop() {
                    Log.d(TAG, "onSwipeTop: Performed")
                    openActivity()
                }

                override fun onSwipeLeft() {
                    nextPrevMusic(increment = true)
                }

                override fun onSwipeRight() {
                    nextPrevMusic(increment = false)
                }
            })

            Glide.with(this)
                .load(getImageArt(MusicInterface.musicList[MusicInterface.songPosition].path))
                .apply(
                    RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
                ).into(binding.fragmentImage)
            binding.fragmentTitle.text =
                MusicInterface.musicList[MusicInterface.songPosition].title
            binding.fragmentAlbumName.text =
                MusicInterface.musicList[MusicInterface.songPosition].album
            if (MusicInterface.isPlaying) binding.fragmentButton.setImageResource(R.drawable.pause_now)
            else binding.fragmentButton.setImageResource(R.drawable.play_now)

        }
    }

    private fun playMusic() {
        MusicInterface.musicService!!.audioManager.requestAudioFocus(
            MusicInterface.musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        MusicInterface.isPlaying = true
        binding.fragmentButton.setImageResource(R.drawable.pause_now)
        MusicInterface.musicService!!.showNotification(R.drawable.pause_notification)
        MusicInterface.binding.interfacePlay.setImageResource(R.drawable.pause)
        MusicInterface.musicService!!.mediaPlayer!!.start()

    }

    private fun pauseMusic() {
        MusicInterface.musicService!!.audioManager.abandonAudioFocus(MusicInterface.musicService)
        MusicInterface.isPlaying = false
        MusicInterface.musicService!!.mediaPlayer!!.pause()
        MusicInterface.musicService!!.showNotification(R.drawable.play_notification)
        MusicInterface.binding.interfacePlay.setImageResource(R.drawable.play)
        binding.fragmentButton.setImageResource(R.drawable.play_now)

    }

    private fun nextPrevMusic(increment: Boolean) {
        setSongPosition(increment = increment)
        MusicInterface.musicService!!.initSong()
        Glide.with(requireContext())
            .load(getImageArt(MusicInterface.musicList[MusicInterface.songPosition].path))
            .apply(
                RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop()
            ).into(binding.fragmentImage)
        binding.fragmentTitle.text =
            MusicInterface.musicList[MusicInterface.songPosition].title
        binding.fragmentAlbumName.text =
            MusicInterface.musicList[MusicInterface.songPosition].album
        playMusic()
    }

    fun openActivity() {
        val intent = Intent(requireContext(), MusicInterface::class.java)
        intent.putExtra("index", MusicInterface.songPosition)
        intent.putExtra("class", "Now playing")
        ContextCompat.startActivity(requireContext(), intent, null)
    }
}
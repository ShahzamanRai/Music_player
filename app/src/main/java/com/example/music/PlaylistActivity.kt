package com.example.music

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.music.databinding.ActivityPlaylistBinding

class PlaylistActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlaylistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener {
            finish()
        }
    }
}
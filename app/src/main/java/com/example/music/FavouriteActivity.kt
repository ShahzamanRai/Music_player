package com.example.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.music.databinding.ActivityFavouriteBinding

class FavouriteActivity : AppCompatActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityFavouriteBinding
        var favSongList: ArrayList<MusicClass> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        val recyclerView = binding.listViewFA
        recyclerView.adapter = FavouriteAdapter(this, favSongList)
        recyclerView.setItemViewCacheSize(50)
        recyclerView.hasFixedSize()
        recyclerView.layoutManager = LinearLayoutManager(this@FavouriteActivity)
        if (favSongList.size < 1) binding.floatingActionButton.visibility = View.INVISIBLE
        binding.floatingActionButton.setOnClickListener {
            shuffleSongs()
        }
    }

    fun shuffleSongs() {
        val intent = Intent(this, MusicInterface::class.java)
        intent.putExtra("index", 0)
        intent.putExtra("class", "FavouriteShuffle")
        startActivity(intent)
    }
}
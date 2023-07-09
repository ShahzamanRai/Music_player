package com.example.music.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.music.adapters.FavouriteAdapter
import com.example.music.MusicClass
import com.example.music.R
import com.example.music.checkPlaylist
import com.example.music.databinding.ActivityFavouriteBinding

class FavouriteActivity : AppCompatActivity() {
    private lateinit var adapter : FavouriteAdapter
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityFavouriteBinding
        var favSongList: ArrayList<MusicClass> = ArrayList()
        lateinit var musicListSearch: java.util.ArrayList<MusicClass>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        if (favSongList.isNotEmpty()) {
            setTheme(R.style.Theme_Music)
        }
        setContentView(binding.root)
        favSongList = checkPlaylist(favSongList)
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null) {
                    val userInput = newText.lowercase()
                    for (song in favSongList)
                        if (song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    MainActivity.isSearching = true
                    adapter.updateMusicList(searchList = musicListSearch)
                }
                return true
            }
        })

    val recyclerView = binding.listViewFA
        adapter = FavouriteAdapter(this, favSongList)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(50)
        recyclerView.hasFixedSize()
        recyclerView.layoutManager = LinearLayoutManager(this@FavouriteActivity)
        if (favSongList.size < 1) binding.floatingActionButton.visibility = View.INVISIBLE
        binding.floatingActionButton.setOnClickListener {
            shuffleSongs()
        }
    }

    private fun shuffleSongs() {
        val intent = Intent(this, MusicInterface::class.java)
        intent.putExtra("index", 0)
        intent.putExtra("class", "FavouriteShuffle")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
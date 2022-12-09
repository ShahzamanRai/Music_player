package com.example.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.music.databinding.ActivityPlaylistDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder

class PlaylistActivityDetails : AppCompatActivity() {

    lateinit var binding: ActivityPlaylistDetailsBinding
    private lateinit var adapter: MusicAdapter

    companion object {
        var currentPlaylistPos: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)
        currentPlaylistPos = intent.extras?.getInt("index") as Int
        try {
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        } catch (e: Exception) {
            return
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.listViewPAD.setItemViewCacheSize(10)
        binding.listViewPAD.setHasFixedSize(true)
        binding.listViewPAD.layoutManager = LinearLayoutManager(this)
        adapter = MusicAdapter(
            this,
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist,
            playlistDetails = true
        )
        binding.listViewPAD.adapter = adapter

    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        binding.PlaylistNamePAD.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name
        binding.PlaylistSizePAD.text = adapter.itemCount.toString() + " Songs"
        binding.PlaylistDatePAD.text =
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn
        binding.PlaylistCreatedByPAD.text =
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdBy
        if (adapter.itemCount > 0) {
            val myOptions = RequestOptions()
                .centerCrop()
                .override(150, 150)
            Glide
                .with(this)
                .applyDefaultRequestOptions(myOptions)
                .load(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].artUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.image_as_cover)
                .into(binding.imagePAD)
            binding.headerTitle.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name
            binding.floatingActionButton.visibility = View.VISIBLE
        }
        binding.floatingActionButton.setOnClickListener {
            val intent = Intent(this, MusicInterface::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }
        binding.addPAD.root.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
        }
        binding.RemovePAD.root.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Remove")
                .setMessage("Do you want to remove all songs from playlist?")
                .setPositiveButton("Yes") { dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
                    adapter.refreshPlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()

        }
        adapter.notifyDataSetChanged()
        //for storing favourites data using shared preferences
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
    }
}
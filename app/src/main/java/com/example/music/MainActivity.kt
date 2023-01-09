package com.example.music

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var musicAdapter: MusicAdapter
    private lateinit var toggle: ActionBarDrawerToggle

    companion object {
        lateinit var songList: ArrayList<MusicClass>
        lateinit var recyclerView: RecyclerView
        lateinit var musicListSearch: ArrayList<MusicClass>
        var isSearching: Boolean = false
        var playNextList: ArrayList<MusicClass> = ArrayList()

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMainBinding
        var sortOrder: Int = 0
        val sortingList = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE + " DESC",
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_Music)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (requestRuntimePermission()) {
            init()
            FavouriteActivity.favSongList = ArrayList()
            val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
            val jsonString = editor.getString("FavouriteSongs", null)
            val typeToken = object : TypeToken<ArrayList<MusicClass>>() {}.type
            if (jsonString != null) {
                val data: ArrayList<MusicClass> =
                    GsonBuilder().create().fromJson(jsonString, typeToken)
                FavouriteActivity.favSongList.addAll(data)
            }
            PlaylistActivity.musicPlaylist = MusicPlaylist()
            val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
            if (jsonStringPlaylist != null) {
                val dataPlaylist: MusicPlaylist =
                    GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
                PlaylistActivity.musicPlaylist = dataPlaylist
            }
        }


//for nav drawer
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.likedSongs.root.setOnClickListener {
            val intent = Intent(baseContext, FavouriteActivity::class.java)
            startActivity(intent)
        }
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navFeedback -> {
                    startActivity(Intent(this, FeedbackActivity::class.java))
                }
                R.id.navAbout -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.navExit -> {
                    val builder = MaterialAlertDialogBuilder(this)
                    builder.setTitle("Exit")
                        .setMessage("Do you want to close app?")
                        .setPositiveButton("Yes") { _, _ ->
                            exitApplication()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                    val customDialog = builder.create()
                    customDialog.show()
                }
            }
            true
        }
        binding.playlistView.root.setOnClickListener {
            val intent = Intent(baseContext, PlaylistActivity::class.java)
            startActivity(intent)
        }


        binding.sort.setOnClickListener {
            val menuList = arrayOf("Title", "Size", "Recently added")
            var currentSort = sortOrder
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Sorting")
                .setPositiveButton("Sort") { _, _ ->
                    val editor = getSharedPreferences("SORTING", MODE_PRIVATE).edit()
                    editor.putInt("sortOrder", currentSort)
                    editor.apply()
                    init()
                }
                .setSingleChoiceItems(menuList, currentSort) { _, which ->
                    currentSort = which
                }
            val customDialog = builder.create()
            customDialog.show()
        }
        //for refreshing layout on swipe from top
        binding.refreshLayout.setRefreshView(WaveAnimation(this@MainActivity))
        binding.refreshLayout.setOnRefreshListener(object :
            SSPullToRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1500)
                    songList = getAudio()

                    musicAdapter.updateMusicList(songList)
                    binding.refreshLayout.setRefreshing(false) // This stops refreshing
                }
            }
        })

        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null) {
                    val userInput = newText.lowercase()
                    for (song in songList)
                        if (song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    isSearching = true
                    musicAdapter.updateMusicList(searchList = musicListSearch)
                }
                return true
            }
        })
    }

    @SuppressLint("Recycle", "Range")
    private fun getAudio(): ArrayList<MusicClass> {
        val tempList = ArrayList<MusicClass>()

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )


        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortingList[sortOrder],
            null
        )

        if (cursor != null) {
            if (cursor.moveToNext()) do {
                val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                val durationC =
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                val albumIdC =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                        .toString()
                val uri = Uri.parse("content://media/external/audio/albumart")
                val artUiC = Uri.withAppendedPath(uri, albumIdC).toString()
                val music = MusicClass(
                    id = idC,
                    title = titleC,
                    album = albumC,
                    length = durationC,
                    artist = artistC,
                    path = pathC,
                    artUri = artUiC
                )
                val file = File(music.path)
                if (file.exists()) {
                    tempList.add(music)
                }

            } while (cursor.moveToNext())
            cursor.close()
        }
        return tempList
    }


    private fun requestRuntimePermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 3
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            init()

        } else {
            requestRuntimePermission()
        }

    }

    private fun init() {

        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        sortOrder = sortEditor.getInt("sortOrder", 0)
        songList = getAudio()
        recyclerView = binding.listView


        musicAdapter = MusicAdapter(this, songList)
        recyclerView.adapter = musicAdapter
        recyclerView.setItemViewCacheSize(50)
        recyclerView.hasFixedSize()
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

    }

    override fun onDestroy() {
        super.onDestroy()
        exitApplication()
    }

    override fun onResume() {
        super.onResume()
        //for sorting
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        val sortValue = sortEditor.getInt("sortOrder", 0)
        if (sortOrder != sortValue) {
            sortOrder = sortValue
            songList = getAudio()
            musicAdapter.updateMusicList(songList)
        }
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favSongList)
        editor.putString("FavouriteSongs", jsonString)
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
        if (MusicInterface.musicService != null) binding.nowPlaying.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)

    }
}

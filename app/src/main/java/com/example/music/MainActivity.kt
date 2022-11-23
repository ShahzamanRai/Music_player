package com.example.music

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var musicAdapter: MusicAdapter

    companion object {
        lateinit var songList: ArrayList<MusicClass>
        lateinit var recyclerView: RecyclerView
        lateinit var filteredList: ArrayList<MusicClass>
        var isSearching: Boolean = false

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
        }

        binding.likedSongs.root.setOnClickListener {
            val intent = Intent(baseContext, FavouriteActivity::class.java)
            startActivity(intent)
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

        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                filterList(s)
                return false
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


    private fun filterList(text: String) {
        if (text.trim { it <= ' ' }.isNotEmpty()) {
            filteredList = ArrayList<MusicClass>()
            for (song: MusicClass in songList) {
                if (song.title.lowercase(Locale.ROOT)
                        .contains(text.lowercase(Locale.getDefault()))
                ) {
                    filteredList.add(song)
                }
                recyclerView.adapter = MusicAdapter(this, filteredList)
                isSearching = true
            }
        } else {
            recyclerView.adapter = MusicAdapter(this, songList)
        }
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
        if (!MusicInterface.isPlaying && MusicInterface.musicService != null) {
            exitApplication()

        }

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
        editor.apply()
    }

}

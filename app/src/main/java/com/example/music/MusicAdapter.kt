package com.example.music

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.music.databinding.SingleLayoutBinding

class MusicAdapter(
    private val context: Context,
    private var musicList: ArrayList<MusicClass>,
    private val playlistDetails: Boolean = false,
    private val selectionActivity: Boolean = false
) :
    RecyclerView.Adapter<MusicAdapter.MyHolder>() {

    class MyHolder(binding: SingleLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val titleView = binding.titleView
        val albumName = binding.albumName
        val imageView = binding.imageView
        val duration = binding.duration
        val root = binding.root
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            SingleLayoutBinding.inflate(
                LayoutInflater.from(
                    context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.titleView.text = musicList[position].title
        holder.albumName.text = musicList[position].artist
        holder.duration.text = formatDuration(musicList[position].length)
        val myOptions = RequestOptions()
            .centerCrop()
            .override(100, 100)

        Glide
            .with(context)
            .applyDefaultRequestOptions(myOptions)
            .load(musicList[position].artUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.image_as_cover)
            .into(holder.imageView)
        when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(position = position, parameter = "PlaylistDetailsAdapter")
                }
            }

            selectionActivity -> {
                holder.root.setOnClickListener {
                    if (addSong(musicList[position]))
                        holder.root.setBackgroundResource(R.drawable.bg_selection)
                    else
                        holder.root.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.unSelectBG
                            )
                        )

                }
            }
            else -> {
                holder.itemView.setOnClickListener {
                    if (MainActivity.isSearching)
                        sendIntent(position = position, parameter = "MusicAdapterSearch")
                    else
                        sendIntent(position = position, parameter = "MusicAdapter")
                }
            }
        }
    }


    override fun getItemCount(): Int {
        return musicList.size
    }

    private fun sendIntent(position: Int, parameter: String) {
        val intent = Intent(context, MusicInterface::class.java)
        intent.putExtra("index", position)
        intent.putExtra("class", parameter)
        ContextCompat.startActivity(context, intent, null)
    }

    fun updateMusicList(searchList: ArrayList<MusicClass>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun addSong(song: MusicClass): Boolean {
        PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if (song.id == music.id) {
                PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist.removeAt(
                    index
                )
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist.add(
            song
        )
        return true
    }

    fun refreshPlaylist() {
        musicList = ArrayList()
        musicList =
            PlaylistActivity.musicPlaylist.ref[PlaylistActivityDetails.currentPlaylistPos].playlist
        notifyDataSetChanged()
    }

}

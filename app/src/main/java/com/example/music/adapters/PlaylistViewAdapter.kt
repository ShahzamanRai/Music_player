package com.example.music.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.music.Playlist
import com.example.music.R
import com.example.music.activities.PlaylistActivity
import com.example.music.activities.PlaylistActivityDetails
import com.example.music.databinding.PlaylistViewDesignBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistViewAdapter(
    private val context: Context,
    private var playlistList: ArrayList<Playlist>
) :
    RecyclerView.Adapter<PlaylistViewAdapter.MyHolder>() {

    class MyHolder(binding: PlaylistViewDesignBinding) : RecyclerView.ViewHolder(binding.root) {
        val playlistName = binding.albumName
        val imageView = binding.cover
        val root = binding.root
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            PlaylistViewDesignBinding.inflate(
                LayoutInflater.from(
                    context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.playlistName.text = playlistList[position].name
        holder.playlistName.isSelected = true
        holder.root.setOnClickListener {
            val intent = Intent(context, PlaylistActivityDetails::class.java)
            intent.putExtra("index", position)
            ContextCompat.startActivity(context, intent, null)
        }
        holder.root.setOnLongClickListener {
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(playlistList[position].name)
                .setMessage("Do you want to delete playlist?")
                .setPositiveButton("Yes") { dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref.removeAt(position)
                    refreshPlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()
            return@setOnLongClickListener true
        }
        if (PlaylistActivity.musicPlaylist.ref[position].playlist.size > 0) {
            Glide.with(context)
                .load(PlaylistActivity.musicPlaylist.ref[position].playlist[0].artUri)
                .apply(
                    RequestOptions().placeholder(R.drawable.image_as_cover)
                        .centerCrop()
                )
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int {
        return playlistList.size
    }

    fun refreshPlaylist() {
        playlistList = ArrayList()
        playlistList.addAll(PlaylistActivity.musicPlaylist.ref)
        notifyDataSetChanged()
    }

}
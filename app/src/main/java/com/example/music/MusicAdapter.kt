package com.example.music

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.music.databinding.SingleLayoutBinding

class MusicAdapter(private val context: Context, private var musicList: ArrayList<MusicClass>) :
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
        try {

            holder.titleView.text = musicList[position].title
            holder.albumName.text = musicList[position].album
            holder.duration.text = formatDuration(musicList[position].length)
            Glide
                .with(context)
                .load(getImageArt(musicList[position].path))
                .apply(RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop())
                .into(holder.imageView)
            holder.itemView.setOnClickListener {
                if (MainActivity.isSearching)
                    sendIntent(position = position, parameter = "MusicAdapterSearch")
                else
                    sendIntent(position = position, parameter = "MusicAdapter")
            }
        } catch (e: Exception) {
            Log.e("AdapterView", e.toString())
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
}

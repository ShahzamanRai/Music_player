package com.example.music

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class MusicAdapter(private val context: Context, private var musicList: ArrayList<MusicClass>) :
    RecyclerView.Adapter<MusicAdapter.ViewHolder>() {


    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleView: TextView = itemView.findViewById(R.id.titleView)
        val duration: TextView = itemView.findViewById(R.id.duration)
        val albumName: TextView = itemView.findViewById(R.id.albumName)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.single_layout, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleView.text = musicList[position].title
        holder.albumName.text = musicList[position].album
        holder.duration.text = formatDuration(musicList[position].length)
        Glide
            .with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.image_as_cover).centerCrop())
            .into(holder.imageView)
        holder.itemView.setOnClickListener {
            if (MainActivity.isSearching)
                sendIntent(position = position, parameter = "MusicAdapterSearch")
            else
                sendIntent(position = position, parameter = "MusicAdapter")
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

package com.example.vk_video_loader

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class VideoFragment (var name: String, var description: String, var potw : Boolean, var repeat: Boolean, var compress: Boolean) {}

class VideoFragmentAdapter(private val context: Context, private val videoFragmentsList: MutableList<VideoFragment>):
    RecyclerView.Adapter<VideoFragmentAdapter.VideoFragmentHolder>() {

        class VideoFragmentHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var name_txt: TextView = itemView.findViewById(R.id.fragment_name)
            var description_txt: TextView = itemView.findViewById(R.id.fragment_description)
            var potw_txt: TextView = itemView.findViewById(R.id.fragment_potw)
            var repeat_txt: TextView = itemView.findViewById(R.id.fragment_repeat)
            var compress_txt: TextView = itemView.findViewById(R.id.fragment_compress)

            fun bind() {
                itemView.setOnClickListener {
                    val intent = Intent(it.context, VideoDetailsActivity::class.java)
                    intent.putExtra("video_name", name_txt.text)
                    intent.putExtra("video_description", description_txt.text)
                    intent.putExtra("video_potw", potw_txt.text)
                    intent.putExtra("video_repeat", repeat_txt.text)
                    intent.putExtra("video_compress", compress_txt.text)
                    it.context.startActivity(intent)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFragmentHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.main_video_fragment, parent, false)
        return VideoFragmentHolder(itemView)
    }

    override fun getItemCount() = videoFragmentsList.size

    override fun onBindViewHolder(holder: VideoFragmentHolder, position: Int) {
        val listItem = videoFragmentsList[position]
        holder.bind()

        holder.name_txt.text = listItem.name
        holder.description_txt.text = listItem.description
        holder.potw_txt.text = listItem.potw.toString()
        holder.repeat_txt.text = listItem.repeat.toString()
        holder.compress_txt.text = listItem.compress.toString()
    }

}

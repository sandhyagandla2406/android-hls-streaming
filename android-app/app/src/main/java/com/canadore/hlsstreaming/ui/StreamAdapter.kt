package com.canadore.hlsstreaming.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.canadore.hlsstreaming.R
import com.canadore.hlsstreaming.databinding.ItemStreamBinding
import com.canadore.hlsstreaming.model.DrmType
import com.canadore.hlsstreaming.model.StreamItem
import java.util.Locale

class StreamAdapter(
    private val onStreamClick: (StreamItem) -> Unit
) : ListAdapter<StreamItem, StreamAdapter.StreamViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val binding = ItemStreamBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StreamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StreamViewHolder(
        private val binding: ItemStreamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stream: StreamItem) {
            binding.tvTitle.text = stream.title
            binding.tvDescription.text = stream.description
            binding.tvDuration.text = formatDuration(stream.durationSeconds)

            // DRM badge
            when (stream.drmType) {
                DrmType.NONE      -> binding.chipDrm.visibility = android.view.View.GONE
                DrmType.WIDEVINE  -> {
                    binding.chipDrm.visibility = android.view.View.VISIBLE
                    binding.chipDrm.text = "Widevine"
                }
                DrmType.CLEARKEY  -> {
                    binding.chipDrm.visibility = android.view.View.VISIBLE
                    binding.chipDrm.text = "ClearKey"
                }
            }

            // Thumbnail
            if (stream.thumbnailUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(stream.thumbnailUrl)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            }

            binding.root.setOnClickListener { onStreamClick(stream) }
        }

        private fun formatDuration(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<StreamItem>() {
        override fun areItemsTheSame(a: StreamItem, b: StreamItem) = a.id == b.id
        override fun areContentsTheSame(a: StreamItem, b: StreamItem) = a == b
    }
}

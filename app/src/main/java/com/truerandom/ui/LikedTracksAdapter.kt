package com.truerandom.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.truerandom.R
import com.truerandom.databinding.ItemLikedTrackBinding
import com.truerandom.model.LikedTrackWithCount
import com.truerandom.util.EventsUtil
import com.truerandom.util.FormatUtil.toFormattedTime

class LikedTracksAdapter : PagingDataAdapter<LikedTrackWithCount, LikedTracksAdapter.TrackViewHolder>(
    DiffCallback
) {
    // Update UI when this item changes to / from currently playing item
    private var currentPlayingUri: String? = null
    fun setCurrentPlayingUri(uri: String?) {
        val oldUri = currentPlayingUri
        currentPlayingUri = uri

        // Optimization: Find only the rows that need to change color
        snapshot().items.forEachIndexed { index, track ->
            if (track.trackUri == oldUri || track.trackUri == uri) {
                // We pass a "payload" (a simple boolean) to avoid a full re-bind
                notifyItemChanged(index, "PAYLOAD_COLOR_CHANGE")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemLikedTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item, item.trackUri == currentPlayingUri)
        }
    }

    // This handles the partial update (the "Payload")
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("PAYLOAD_COLOR_CHANGE")) {
            val item = getItem(position)
            holder.updateColors(item?.trackUri == currentPlayingUri)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class TrackViewHolder(private val binding: ItemLikedTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: LikedTrackWithCount, isPlaying: Boolean) {
            with(binding) {
                tvTitle.text = track.trackName
                tvArtist.text = track.artistName
                tvPlayCount.text = track.playCount.toString()

                // Format duration text from ms
                tvDuration.text = track.durationMs.toFormattedTime()

                updateColors(isPlaying)

                // Load Album Cover
                if (track.albumCoverUrl?.isNotBlank() == true) {
                    Glide.with(ivCover.context)
                        .load(track.albumCoverUrl)
                        .placeholder(R.drawable.ic_placeholder_track) // Optional: grey music icon
                        .error(R.drawable.ic_placeholder_track)             // Optional: if URL is broken
                        .centerCrop()                                 // Fills the square nicely
                        .into(ivCover)
                }

                // Start playing this track when tapped
                itemView.setOnClickListener {
                    EventsUtil.sendPlayTrackEvent(track.trackUri)
                }
            }
        }

        // Dedicated function to only touch colors
        fun updateColors(isPlaying: Boolean) {
            val textColourId = if (isPlaying) {
                R.color.purple_200
            } else {
                android.R.color.white
            }

            val textColour = binding.root.context.getColor(textColourId)
            with (binding) {
                tvTitle.setTextColor(textColour)
                tvDuration.setTextColor(textColour)
                tvArtist.setTextColor(textColour)
                tvPlayCount.setTextColor(textColour)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<LikedTrackWithCount>() {
        override fun areItemsTheSame(oldItem: LikedTrackWithCount, newItem: LikedTrackWithCount): Boolean =
            oldItem.trackUri == newItem.trackUri

        override fun areContentsTheSame(oldItem: LikedTrackWithCount, newItem: LikedTrackWithCount): Boolean =
            oldItem == newItem
    }
}
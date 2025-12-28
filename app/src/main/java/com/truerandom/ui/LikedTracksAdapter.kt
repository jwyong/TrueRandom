package com.truerandom.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.truerandom.databinding.ItemLikedTrackBinding
import com.truerandom.model.LikedTrackWithCount

class LikedTracksAdapter : PagingDataAdapter<LikedTrackWithCount, LikedTracksAdapter.TrackViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemLikedTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    class TrackViewHolder(private val binding: ItemLikedTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: LikedTrackWithCount) {
            Log.d(TAG, "TrackViewHolder, bind: trackName = ${track.trackName}")

            with(binding) {
                tvTitle.text = track.trackName
                tvArtist.text = track.artistName
                tvPlayCount.text = track.playCount.toString()

                // Toggle "Local" dot visibility
                tvLocal.visibility = if (track.isLocal == true) View.VISIBLE else View.GONE

                // TODO: Use Glide or Coil to load track.albumCoverUrl into ivCover
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
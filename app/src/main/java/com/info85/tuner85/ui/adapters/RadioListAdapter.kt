package com.info85.tuner85.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.info85.tuner85.R
import com.info85.tuner85.data.model.RadioStation
import com.info85.tuner85.databinding.ItemRadioListBinding

class RadioListAdapter(
    private val onItemClick: (RadioStation, Int) -> Unit
) : ListAdapter<RadioStation, RadioListAdapter.ViewHolder>(DiffCallback()) {

    private var currentStation: RadioStation? = null

    fun setCurrentStation(station: RadioStation?) {
        val old = currentStation
        currentStation = station
        val list = currentList
        val oldPos = if (old != null) list.indexOf(old) else -1
        val newPos = if (station != null) list.indexOf(station) else -1
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRadioListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemRadioListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: RadioStation, position: Int) {
            binding.tvRadioName.text = station.name
            binding.ivRadioLogo.load(station.logoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_radio)
                error(R.drawable.ic_radio)
            }
            binding.root.isSelected = station == currentStation
            binding.root.setOnClickListener {
                onItemClick(station, position)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RadioStation>() {
        override fun areItemsTheSame(oldItem: RadioStation, newItem: RadioStation) =
            oldItem.streamUrl == newItem.streamUrl

        override fun areContentsTheSame(oldItem: RadioStation, newItem: RadioStation) =
            oldItem == newItem
    }
}

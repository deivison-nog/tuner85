package com.info85.tuner85.ui.player

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.info85.tuner85.MainActivity
import com.info85.tuner85.R
import com.info85.tuner85.databinding.FragmentPlayerBinding
import com.info85.tuner85.ui.adapters.RadioHorizontalAdapter

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by activityViewModels()
    private lateinit var horizontalAdapter: RadioHorizontalAdapter
    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupMenuButton()
        setupRecyclerView()
        setupControls()
        setupVolumeControl()
        observeViewModel()
    }

    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener {
            (requireActivity() as? MainActivity)?.openDrawer()
        }
    }

    private fun setupRecyclerView() {
        horizontalAdapter = RadioHorizontalAdapter { station, index ->
            viewModel.selectStation(station, index)
        }
        binding.rvOtherStations.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = horizontalAdapter
        }
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            viewModel.playPause()
        }
        binding.btnNext.setOnClickListener {
            viewModel.nextStation()
        }
        binding.btnPrevious.setOnClickListener {
            viewModel.previousStation()
        }
    }

    private fun setupVolumeControl() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.max = maxVolume
        binding.seekBarVolume.progress = currentVolume

        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnVolumeDown.setOnClickListener {
            val vol = (binding.seekBarVolume.progress - 1).coerceAtLeast(0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
            binding.seekBarVolume.progress = vol
        }

        binding.btnVolumeUp.setOnClickListener {
            val vol = (binding.seekBarVolume.progress + 1).coerceAtMost(maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
            binding.seekBarVolume.progress = vol
        }
    }

    private var fullStationList: List<com.info85.tuner85.data.model.RadioStation> = emptyList()

    private fun updateOtherStations() {
        val current = viewModel.currentStation.value
        val others = fullStationList.filter { it != current }.shuffled().take(5)
        horizontalAdapter.submitList(others)
    }

    private fun observeViewModel() {
        viewModel.currentStation.observe(viewLifecycleOwner) { station ->
            if (station != null) {
                binding.tvRadioName.text = station.name
                binding.tvRadioNameLarge.text = station.name
                binding.ivRadioLogo.load(station.logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.logo)
                    error(R.drawable.logo)
                    transformations(CircleCropTransformation())
                }
                horizontalAdapter.setCurrentStation(station)
                updateOtherStations()
            }
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        viewModel.radioList.observe(viewLifecycleOwner) { stations ->
            fullStationList = stations
            updateOtherStations()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

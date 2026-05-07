package com.info85.tuner85.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.info85.tuner85.data.model.RadioStation
import com.info85.tuner85.data.repository.RadioRepository
import com.info85.tuner85.service.RadioService
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository()

    private val _radioList = MutableLiveData<List<RadioStation>>(emptyList())
    val radioList: LiveData<List<RadioStation>> = _radioList

    private val _currentStation = MutableLiveData<RadioStation?>()
    val currentStation: LiveData<RadioStation?> = _currentStation

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var radioService: RadioService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioService.RadioBinder
            radioService = binder.getService().also { svc ->
                svc.onPlaybackStateChanged = { isPlaying ->
                    _isPlaying.postValue(isPlaying)
                }
                svc.onStationChanged = { station ->
                    _currentStation.postValue(station)
                }
                svc.onError = { message ->
                    _error.postValue(message)
                }

                // Sync state
                _isPlaying.postValue(svc.isPlaying())
                _currentStation.postValue(svc.currentStation)

                // Load stations
                if (_radioList.value.isNullOrEmpty()) {
                    loadStations()
                }
            }
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            radioService = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, RadioService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun loadStations() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            repository.getRadioStations().fold(
                onSuccess = { stations ->
                    _radioList.postValue(stations)
                    _isLoading.postValue(false)
                    radioService?.stationList = stations
                    if (stations.isNotEmpty() && _currentStation.value == null) {
                        selectStation(stations[0], 0)
                    }
                },
                onFailure = { e ->
                    _error.postValue(e.message ?: "Failed to load stations")
                    _isLoading.postValue(false)
                }
            )
        }
    }

    fun playPause() {
        radioService?.let { service ->
            if (service.isPlaying()) {
                service.pause()
            } else {
                if (service.currentStation != null) {
                    service.play()
                } else {
                    val stations = _radioList.value
                    if (!stations.isNullOrEmpty()) {
                        selectStation(stations[0], 0)
                    }
                }
            }
        }
    }

    fun nextStation() {
        radioService?.nextStation()
    }

    fun previousStation() {
        radioService?.previousStation()
    }

    fun selectStation(station: RadioStation, index: Int) {
        radioService?.let { service ->
            service.stationList = _radioList.value ?: emptyList()
            service.currentIndex = index
            service.playStation(station)
        }
    }

    fun clearError() {
        _error.value = null
    }
}

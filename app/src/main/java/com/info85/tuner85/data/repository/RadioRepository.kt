package com.info85.tuner85.data.repository

import com.info85.tuner85.data.model.RadioStation
import com.info85.tuner85.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadioRepository {

    private val parser = M3UParser()
    private val m3uUrl = "https://ontvadmin.info85.com.br/files/radios.m3u"

    suspend fun getRadioStations(): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        try {
            val stations = parser.parseFromUrl(m3uUrl)
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.info85.tuner85.data.parser

import com.info85.tuner85.data.model.RadioStation
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class M3UParser {

    private val client = OkHttpClient()

    suspend fun parseFromUrl(url: String): List<RadioStation> {
        val content = downloadM3U(url)
        return parseContent(content)
    }

    private fun downloadM3U(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    private fun parseContent(content: String): List<RadioStation> {
        val stations = mutableListOf<RadioStation>()
        val lines = content.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF")) {
                val name = extractName(line)
                val logoUrl = extractLogoUrl(line)

                // Find the stream URL on next non-empty, non-comment line
                var j = i + 1
                while (j < lines.size && (lines[j].trim().isEmpty() || lines[j].trim().startsWith("#"))) {
                    j++
                }

                if (j < lines.size) {
                    val streamUrl = lines[j].trim()
                    if (streamUrl.isNotEmpty() && (streamUrl.startsWith("http") || streamUrl.startsWith("rtsp"))) {
                        stations.add(
                            RadioStation(
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = logoUrl
                            )
                        )
                    }
                    i = j + 1
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        return stations
    }

    private fun extractName(extinf: String): String {
        val commaIndex = extinf.lastIndexOf(',')
        return if (commaIndex >= 0 && commaIndex < extinf.length - 1) {
            extinf.substring(commaIndex + 1).trim()
        } else {
            "Unknown Radio"
        }
    }

    private fun extractLogoUrl(extinf: String): String {
        val tvgLogoRegex = Regex("""tvg-logo="([^"]*?)"""")
        return tvgLogoRegex.find(extinf)?.groupValues?.get(1) ?: ""
    }
}

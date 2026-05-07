package com.info85.tuner85.data.model

data class RadioStation(
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val genre: String? = null
)

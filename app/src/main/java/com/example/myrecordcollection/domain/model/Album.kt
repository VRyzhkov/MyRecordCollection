package com.example.myrecordcollection.domain.model

data class Album(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val albumUrl: String? = null,
)

package com.example.myrecordcollection.domain.model

data class ArtistGroup(
    val artist: Artist,
    val albums: List<Album>,
)

package com.example.myrecordcollection.data.local

data class AlbumRow(
    val albumId: String,
    val albumTitle: String,
    val remoteCoverUrl: String?,
    val localCoverPath: String?,
    val artistId: String,
    val artistName: String,
    val groupOrder: Int,
    val albumOrder: Int,
)

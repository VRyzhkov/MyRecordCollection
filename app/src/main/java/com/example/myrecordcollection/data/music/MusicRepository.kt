package com.example.myrecordcollection.data.music

import com.example.myrecordcollection.domain.model.Album

interface MusicRepository {
    suspend fun getFavoriteAlbums(): List<Album>
}

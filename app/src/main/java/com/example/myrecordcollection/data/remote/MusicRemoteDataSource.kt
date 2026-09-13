package com.example.myrecordcollection.data.remote

import com.example.myrecordcollection.domain.model.Album

interface MusicRemoteDataSource {
    suspend fun getCollection(): List<Album>
}

class InvalidMusicTokenException(message: String) : Exception(message)

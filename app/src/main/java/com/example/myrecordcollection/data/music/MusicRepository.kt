package com.example.myrecordcollection.data.music

import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.ArtistGroup
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun observeAlbumGroups(): Flow<List<ArtistGroup>>

    suspend fun refreshCollection()
}

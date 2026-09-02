package com.example.myrecordcollection.domain.usecase

import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.ArtistGroup
import kotlinx.coroutines.flow.Flow

class LoadAlbumGroupsUseCase(
    private val musicRepository: MusicRepository,
) {
    operator fun invoke(): Flow<List<ArtistGroup>> = musicRepository.observeAlbumGroups()
}

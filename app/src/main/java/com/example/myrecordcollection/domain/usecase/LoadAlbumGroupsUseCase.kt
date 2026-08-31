package com.example.myrecordcollection.domain.usecase

import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.ArtistGroup

class LoadAlbumGroupsUseCase(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(): List<ArtistGroup> =
        musicRepository
            .getFavoriteAlbums()
            .distinctBy { it.id }
            .mapNotNull { album ->
                album.artists.firstOrNull()?.let { artist -> artist to album }
            }
            .groupBy(
                keySelector = { (artist, _) -> artist.id },
                valueTransform = { it },
            )
            .values
            .map { albumsByArtist ->
                ArtistGroup(
                    artist = albumsByArtist.first().first,
                    albums = albumsByArtist
                        .map { (_, album) -> album }
                        .sortedBy { it.title.lowercase() },
                )
            }
            .sortedBy { it.artist.name.lowercase() }
}

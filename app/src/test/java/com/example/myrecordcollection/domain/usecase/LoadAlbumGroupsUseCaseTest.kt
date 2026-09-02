package com.example.myrecordcollection.domain.usecase

import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.ArtistGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadAlbumGroupsUseCaseTest {
    @Test
    fun groupsUniqueAlbumsByPrimaryArtist() = runBlocking {
        val alpha = Artist(id = "alpha", name = "Alpha")
        val beta = Artist(id = "beta", name = "Beta")
        val albums = listOf(
            Album(id = "beta-album", title = "First", artists = listOf(beta)),
            Album(id = "zulu", title = "Zulu", artists = listOf(alpha)),
            Album(id = "alpha", title = "Alpha", artists = listOf(alpha)),
            Album(id = "alpha", title = "Alpha duplicate", artists = listOf(alpha)),
        )
        val groups = listOf(
            ArtistGroup(alpha, albums.filter { it.artists.first() == alpha }.distinctBy { it.id }.sortedBy { it.title }),
            ArtistGroup(beta, albums.filter { it.artists.first() == beta }),
        )
        val repository = object : MusicRepository {
            override fun observeAlbumGroups(): Flow<List<ArtistGroup>> = flowOf(groups)
            override suspend fun refreshCollection() = Unit
        }

        val result = LoadAlbumGroupsUseCase(repository)().first()

        assertEquals(listOf("Alpha", "Beta"), result.map { it.artist.name })
        assertEquals(listOf("Alpha", "Zulu"), result.first().albums.map { it.title })
    }
}

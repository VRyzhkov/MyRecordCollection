package com.example.myrecordcollection.domain.usecase

import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
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
        val repository = object : MusicRepository {
            override suspend fun getFavoriteAlbums(): List<Album> = albums
        }

        val result = LoadAlbumGroupsUseCase(repository)()

        assertEquals(listOf("Alpha", "Beta"), result.map { it.artist.name })
        assertEquals(listOf("Alpha", "Zulu"), result.first().albums.map { it.title })
    }
}

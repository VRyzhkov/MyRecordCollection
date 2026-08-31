package com.example.myrecordcollection.data.music

import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist

class FakeMusicRepository : MusicRepository {
    override suspend fun getFavoriteAlbums(): List<Album> {
        val pinkFloyd = Artist(id = "artist-pink-floyd", name = "Pink Floyd")
        val queen = Artist(id = "artist-queen", name = "Queen")
        val daftPunk = Artist(id = "artist-daft-punk", name = "Daft Punk")
        val massiveAttack = Artist(id = "artist-massive-attack", name = "Massive Attack")

        return listOf(
            Album(id = "dark-side", title = "The Dark Side of the Moon", artists = listOf(pinkFloyd)),
            Album(id = "wish-you-were-here", title = "Wish You Were Here", artists = listOf(pinkFloyd)),
            Album(id = "the-wall", title = "The Wall", artists = listOf(pinkFloyd)),
            Album(id = "night-at-opera", title = "A Night at the Opera", artists = listOf(queen)),
            Album(id = "jazz", title = "Jazz", artists = listOf(queen)),
            Album(id = "random-access-memories", title = "Random Access Memories", artists = listOf(daftPunk)),
            Album(id = "discovery", title = "Discovery", artists = listOf(daftPunk)),
            Album(id = "mezzanine", title = "Mezzanine", artists = listOf(massiveAttack)),
        )
    }
}

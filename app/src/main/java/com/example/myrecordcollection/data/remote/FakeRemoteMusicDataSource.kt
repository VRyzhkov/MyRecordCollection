package com.example.myrecordcollection.data.remote

import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist

class FakeRemoteMusicDataSource : MusicRemoteDataSource {
    override suspend fun getFavoriteAlbums(): List<Album> {
        val pinkFloyd = Artist(id = "artist-pink-floyd", name = "Pink Floyd")
        val queen = Artist(id = "artist-queen", name = "Queen")
        val daftPunk = Artist(id = "artist-daft-punk", name = "Daft Punk")
        val massiveAttack = Artist(id = "artist-massive-attack", name = "Massive Attack")

        return listOf(
            album("dark-side", "The Dark Side of the Moon", pinkFloyd),
            album("wish-you-were-here", "Wish You Were Here", pinkFloyd),
            album("the-wall", "The Wall", pinkFloyd),
            album("night-at-opera", "A Night at the Opera", queen),
            album("jazz", "Jazz", queen),
            album("random-access-memories", "Random Access Memories", daftPunk),
            album("discovery", "Discovery", daftPunk),
            album("mezzanine", "Mezzanine", massiveAttack),
        )
    }

    private fun album(id: String, title: String, artist: Artist) = Album(
        id = id,
        title = title,
        artists = listOf(artist),
        coverUrl = "https://picsum.photos/seed/$id/600/600",
        albumUrl = "https://music.yandex.ru/album/$id",
    )
}

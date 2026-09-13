package com.example.myrecordcollection.domain.model

/** Reserved group IDs keep playlists separate from performers in the offline collection. */
object CollectionGroups {
    val likedTracks = Artist("collection:liked-tracks", "Моя коллекция")
    val playlists = Artist("collection:playlists", "Мои плейлисты")

    fun ordered(items: List<Album>): List<ArtistGroup> = items
        .distinctBy { it.id }
        .filter { it.artists.isNotEmpty() }
        .groupBy { it.artists.first().id }
        .entries
        .sortedWith(compareBy({ priority(it.key) }, { it.value.first().artists.first().name.lowercase() }))
        .map { (_, albums) ->
            val artist = albums.first().artists.first()
            ArtistGroup(
                artist,
                if (artist.id == likedTracks.id || artist.id == playlists.id) albums
                else albums.sortedBy { it.title.lowercase() },
            )
        }

    private fun priority(id: String): Int = when (id) {
        likedTracks.id -> 0
        playlists.id -> 1
        else -> 2
    }
}

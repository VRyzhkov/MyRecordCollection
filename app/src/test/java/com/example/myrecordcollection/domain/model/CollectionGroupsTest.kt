package com.example.myrecordcollection.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionGroupsTest {
    @Test
    fun pinsSectionsPreservesPlaylistOrderAndSortsOnlyAlbums() {
        val alpha = Artist("alpha", "Alpha")
        val beta = Artist("beta", "Beta")
        val items = listOf(
            Album("b", "B", listOf(beta)),
            Album("pz", "Zulu playlist", listOf(CollectionGroups.playlists)),
            Album("az", "Zulu album", listOf(alpha)),
            Album("likes", "Мне нравится", listOf(CollectionGroups.likedTracks)),
            Album("pa", "Alpha playlist", listOf(CollectionGroups.playlists)),
            Album("aa", "Alpha album", listOf(alpha)),
            Album("aa", "Duplicate", listOf(alpha)),
            Album("invalid", "No artist", emptyList()),
        )
        val groups = CollectionGroups.ordered(items)
        assertEquals(listOf("collection:liked-tracks", "collection:playlists", "alpha", "beta"), groups.map { it.artist.id })
        assertEquals(listOf("likes", "pz", "pa", "aa", "az", "b"), groups.flatMap { it.albums }.map { it.id })
    }
}

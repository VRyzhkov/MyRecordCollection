package com.example.myrecordcollection.data.music

import com.example.myrecordcollection.data.cover.AlbumCoverStorage
import com.example.myrecordcollection.data.local.MusicDao
import com.example.myrecordcollection.data.local.entity.AlbumArtistCrossRef
import com.example.myrecordcollection.data.local.entity.AlbumEntity
import com.example.myrecordcollection.data.local.entity.ArtistEntity
import com.example.myrecordcollection.data.remote.MusicRemoteDataSource
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.ArtistGroup
import com.example.myrecordcollection.domain.model.CollectionGroups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMusicRepository(
    private val musicDao: MusicDao,
    private val coverStorage: AlbumCoverStorage,
    private val remoteDataSource: MusicRemoteDataSource,
) : MusicRepository {
    override fun observeAlbumGroups(): Flow<List<ArtistGroup>> =
        musicDao.observeAlbums().map { rows ->
            rows.groupBy { it.artistId }.values.map { artistRows ->
                val first = artistRows.first()
                val artist = Artist(id = first.artistId, name = first.artistName)
                ArtistGroup(
                    artist = artist,
                    albums = artistRows.map { row ->
                        Album(
                            id = row.albumId,
                            title = row.albumTitle,
                            artists = listOf(artist),
                            coverUrl = row.remoteCoverUrl,
                            localCoverPath = row.localCoverPath,
                            albumUrl = row.albumUrl,
                        )
                    },
                )
            }
        }

    override suspend fun refreshCollection() {
        val groupedAlbums = CollectionGroups.ordered(remoteDataSource.getCollection())
        val remoteAlbums = groupedAlbums.flatMap { it.albums }

        val groupOrderByArtist = groupedAlbums
            .mapIndexed { index, group -> group.artist.id to index }
            .toMap()
        val allArtists = remoteAlbums.flatMap { it.artists }.distinctBy { it.id }
        val artistEntities = allArtists.map { artist ->
            ArtistEntity(
                id = artist.id,
                name = artist.name,
                groupOrder = groupOrderByArtist[artist.id] ?: Int.MAX_VALUE,
            )
        }
        val albumOrderById = groupedAlbums.flatMap { group ->
            group.albums.mapIndexed { index, album -> album.id to index }
        }.toMap()

        val localPaths = mutableSetOf<String>()
        val albumEntities = remoteAlbums.mapNotNull { album ->
            val primaryArtist = album.artists.firstOrNull() ?: return@mapNotNull null
            val localPath = album.coverUrl?.let { url ->
                runCatching { coverStorage.save(album.id, url) }.getOrNull()
            }
            if (localPath != null) localPaths += localPath
            AlbumEntity(
                id = album.id,
                title = album.title,
                primaryArtistId = primaryArtist.id,
                remoteCoverUrl = album.coverUrl,
                localCoverPath = localPath,
                albumUrl = album.albumUrl,
                albumOrder = albumOrderById.getValue(album.id),
            )
        }
        val refs = remoteAlbums.flatMap { album ->
            album.artists.map { artist -> AlbumArtistCrossRef(album.id, artist.id) }
        }

        musicDao.replaceCollection(
            artists = artistEntities,
            albums = albumEntities,
            refs = refs,
            syncedAt = System.currentTimeMillis(),
        )
        coverStorage.removeUnused(localPaths)
    }
}

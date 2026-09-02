package com.example.myrecordcollection.data.music

import com.example.myrecordcollection.data.cover.AlbumCoverStorage
import com.example.myrecordcollection.data.local.MusicDao
import com.example.myrecordcollection.data.local.entity.AlbumArtistCrossRef
import com.example.myrecordcollection.data.local.entity.AlbumEntity
import com.example.myrecordcollection.data.local.entity.ArtistEntity
import com.example.myrecordcollection.data.remote.FakeRemoteMusicDataSource
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.ArtistGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMusicRepository(
    private val musicDao: MusicDao,
    private val coverStorage: AlbumCoverStorage,
    private val remoteDataSource: FakeRemoteMusicDataSource,
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
                        )
                    },
                )
            }
        }

    override suspend fun refreshCollection() {
        val remoteAlbums = remoteDataSource.getFavoriteAlbums().distinctBy { it.id }
        val groupedAlbums = remoteAlbums
            .mapNotNull { album -> album.artists.firstOrNull()?.let { it to album } }
            .groupBy(keySelector = { it.first.id }, valueTransform = { it })
            .values
            .sortedBy { rows -> rows.first().first.name.lowercase() }

        val groupOrderByArtist = groupedAlbums
            .mapIndexed { index, rows -> rows.first().first.id to index }
            .toMap()
        val allArtists = remoteAlbums.flatMap { it.artists }.distinctBy { it.id }
        val artistEntities = allArtists.map { artist ->
            ArtistEntity(
                id = artist.id,
                name = artist.name,
                groupOrder = groupOrderByArtist[artist.id] ?: Int.MAX_VALUE,
            )
        }
        val albumOrderById = groupedAlbums.flatMap { rows ->
            rows.sortedBy { it.second.title.lowercase() }
                .mapIndexed { index, (_, album) -> album.id to index }
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

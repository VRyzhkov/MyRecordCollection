package com.example.myrecordcollection.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myrecordcollection.data.local.entity.AlbumArtistCrossRef
import com.example.myrecordcollection.data.local.entity.AlbumEntity
import com.example.myrecordcollection.data.local.entity.ArtistEntity
import com.example.myrecordcollection.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query(
        """
        SELECT albums.id AS albumId,
               albums.title AS albumTitle,
               albums.remoteCoverUrl,
               albums.localCoverPath,
               artists.id AS artistId,
               artists.name AS artistName,
               artists.groupOrder,
               albums.albumOrder
        FROM albums
        INNER JOIN artists ON artists.id = albums.primaryArtistId
        ORDER BY artists.groupOrder, albums.albumOrder
        """,
    )
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query("SELECT localCoverPath FROM albums WHERE localCoverPath IS NOT NULL")
    suspend fun getLocalCoverPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumArtists(refs: List<AlbumArtistCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)

    @Query("DELETE FROM album_artists")
    suspend fun deleteAlbumArtists()

    @Query("DELETE FROM albums")
    suspend fun deleteAlbums()

    @Query("DELETE FROM artists")
    suspend fun deleteArtists()

    @Transaction
    suspend fun replaceCollection(
        artists: List<ArtistEntity>,
        albums: List<AlbumEntity>,
        refs: List<AlbumArtistCrossRef>,
        syncedAt: Long,
    ) {
        deleteAlbumArtists()
        deleteAlbums()
        deleteArtists()
        insertArtists(artists)
        insertAlbums(albums)
        insertAlbumArtists(refs)
        insertSyncMetadata(SyncMetadataEntity(lastSuccessfulSyncAt = syncedAt))
    }
}

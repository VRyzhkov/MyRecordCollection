package com.example.myrecordcollection.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myrecordcollection.data.local.entity.AlbumArtistCrossRef
import com.example.myrecordcollection.data.local.entity.AlbumEntity
import com.example.myrecordcollection.data.local.entity.ArtistEntity
import com.example.myrecordcollection.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        ArtistEntity::class,
        AlbumEntity::class,
        AlbumArtistCrossRef::class,
        SyncMetadataEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}

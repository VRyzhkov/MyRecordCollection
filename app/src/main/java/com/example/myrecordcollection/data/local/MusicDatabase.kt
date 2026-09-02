package com.example.myrecordcollection.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE albums ADD COLUMN albumUrl TEXT")
            }
        }
    }
}

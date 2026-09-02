package com.example.myrecordcollection.app

import android.app.Application
import androidx.room.Room
import com.example.myrecordcollection.data.cover.AlbumCoverStorage
import com.example.myrecordcollection.data.local.MusicDatabase
import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.data.music.OfflineMusicRepository
import com.example.myrecordcollection.data.remote.FakeRemoteMusicDataSource

class MyRecordCollectionApp : Application() {
    val musicRepository: MusicRepository by lazy {
        val database = Room.databaseBuilder(
            applicationContext,
            MusicDatabase::class.java,
            "music.db",
        ).addMigrations(MusicDatabase.MIGRATION_1_2).build()
        OfflineMusicRepository(
            musicDao = database.musicDao(),
            coverStorage = AlbumCoverStorage(applicationContext),
            remoteDataSource = FakeRemoteMusicDataSource(),
        )
    }
}

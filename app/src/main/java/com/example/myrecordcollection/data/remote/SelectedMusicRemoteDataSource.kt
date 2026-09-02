package com.example.myrecordcollection.data.remote

import android.content.Context
import com.example.myrecordcollection.domain.model.Album

class SelectedMusicRemoteDataSource(
    context: Context,
    private val demo: MusicRemoteDataSource,
    private val yandex: MusicRemoteDataSource,
) : MusicRemoteDataSource {
    private val preferences = context.getSharedPreferences("auth_mode", Context.MODE_PRIVATE)

    override suspend fun getFavoriteAlbums(): List<Album> =
        if (preferences.getBoolean("demo_mode", false)) {
            demo.getFavoriteAlbums()
        } else {
            yandex.getFavoriteAlbums()
        }
}

package com.example.myrecordcollection.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["primaryArtistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("primaryArtistId")],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val primaryArtistId: String,
    val remoteCoverUrl: String?,
    val localCoverPath: String?,
    val albumOrder: Int,
)

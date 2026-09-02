package com.example.myrecordcollection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String = COLLECTION_KEY,
    val lastSuccessfulSyncAt: Long,
) {
    companion object {
        const val COLLECTION_KEY = "collection"
    }
}

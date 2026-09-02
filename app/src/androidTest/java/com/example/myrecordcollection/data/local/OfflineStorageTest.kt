package com.example.myrecordcollection.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myrecordcollection.data.cover.AlbumCoverStorage
import com.example.myrecordcollection.data.local.entity.AlbumArtistCrossRef
import com.example.myrecordcollection.data.local.entity.AlbumEntity
import com.example.myrecordcollection.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OfflineStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java).build()

    @After
    fun tearDown() {
        database.close()
        File(context.filesDir, "album_covers").deleteRecursively()
    }

    @Test
    fun collectionIsReplacedAtomicallyAndObservedInOrder() = runBlocking {
        database.musicDao().replaceCollection(
            artists = listOf(ArtistEntity("artist", "Artist", groupOrder = 0)),
            albums = listOf(
                AlbumEntity("second", "Second", "artist", null, null, null, albumOrder = 1),
                AlbumEntity("first", "First", "artist", null, null, null, albumOrder = 0),
            ),
            refs = listOf(
                AlbumArtistCrossRef("first", "artist"),
                AlbumArtistCrossRef("second", "artist"),
            ),
            syncedAt = 1L,
        )

        assertEquals(
            listOf("first", "second"),
            database.musicDao().observeAlbums().first().map { it.albumId },
        )
    }

    @Test
    fun cleanupKeepsReferencedCoverAndRemovesOrphans() = runBlocking {
        val directory = File(context.filesDir, "album_covers").apply { mkdirs() }
        val used = File(directory, "used.image").apply { writeText("used") }
        val orphan = File(directory, "orphan.image").apply { writeText("orphan") }
        val temporary = File(directory, "partial.download").apply { writeText("partial") }

        AlbumCoverStorage(context).removeUnused(setOf(used.absolutePath))

        assertTrue(used.exists())
        assertFalse(orphan.exists())
        assertFalse(temporary.exists())
    }
}

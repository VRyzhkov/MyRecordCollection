package com.example.myrecordcollection.ui.collection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlbumCarouselTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tappingVisibleSideAlbumOpensThatAlbumAndHiddenAlbumsAreAbsent() {
        val albums = (0..5).map { Album("$it", "Album $it", listOf(Artist("artist", "Artist"))) }
        var opened: Album? = null
        compose.setContent {
            AlbumCarousel(albums, Modifier.fillMaxSize(), onAlbumClick = { opened = it })
        }
        compose.onNodeWithText("Album 1").performTouchInput { click() }
        compose.runOnIdle { assertEquals(albums[1], opened) }
        compose.onNodeWithText("Album 5").assertDoesNotExist()
    }
}

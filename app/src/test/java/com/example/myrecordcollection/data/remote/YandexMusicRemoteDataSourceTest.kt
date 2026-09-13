package com.example.myrecordcollection.data.remote

import com.example.myrecordcollection.data.auth.TokenStorage
import com.example.myrecordcollection.domain.model.CollectionGroups
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class YandexMusicRemoteDataSourceTest {
    private val tokens = object : TokenStorage {
        override fun getAccessToken() = "test-token"
        override fun getRefreshToken(): String? = null
        override fun saveTokens(accessToken: String, refreshToken: String?) = Unit
        override fun clear() = Unit
    }

    private fun source(own: String = "[]", saved: String = "[]", albums: String = "[]") =
        YandexMusicRemoteDataSource(tokens) { url, token ->
            assertEquals("test-token", token)
            JSONObject(when {
                url.endsWith("/account/status") -> """{"result":{"account":{"uid":42}}}"""
                url.endsWith("/playlists/list") -> """{"result":$own}"""
                url.endsWith("/likes/playlists") -> """{"result":$saved}"""
                url.endsWith("/likes/albums?rich=true") -> """{"result":$albums}"""
                else -> error("Unexpected request: $url")
            })
        }

    @Test
    fun collectionContainsOneLikedCardThenOwnAndSavedPlaylistsThenAlbums() = runBlocking {
        val result = source(
            own = """[
                {"kind":3,"title":"Мне нравится"},
                {"kind":7,"title":"Дорога","owner":{"uid":42,"login":"test user"},"cover":{"uri":"//img.test/%%"}}
            ]""",
            saved = """[
                {"playlist":{"kind":7,"title":"Повтор","uid":42}},
                {"playlist":{"kind":7,"title":"Другой владелец","uid":99,"playlistUuid":"uuid-99","cover":{"itemsUri":["img.test/tile/%%"]}}},
                {"playlist":{"kind":3,"title":"Чужой плейлист","uid":99}}
            ]""",
            albums = """[{"album":{"id":7,"title":"Альбом","artists":[{"id":1,"name":"Исполнитель"}]}}]""",
        ).getCollection()

        assertEquals(listOf("liked-tracks:42", "playlist:42:7", "playlist:99:7", "playlist:99:3", "7"), result.map { it.id })
        assertEquals("Мне нравится", result.first().title)
        assertEquals("https://music.yandex.ru/collection/tracks", result.first().albumUrl)
        assertEquals("https://music.yandex.ru/users/test%20user/playlists/7", result[1].albumUrl)
        assertEquals("https://music.yandex.ru/playlists/uuid-99", result[2].albumUrl)
        assertEquals("https://img.test/600x600", result[1].coverUrl)
        assertEquals("https://img.test/tile/600x600", result[2].coverUrl)
        assertEquals("https://music.yandex.ru/album/7", result.last().albumUrl)
    }

    @Test
    fun emptyCollectionStillHasPinnedLikedTracks() = runBlocking {
        val result = source().getCollection()
        assertEquals(1, result.size)
        assertEquals(CollectionGroups.likedTracks, result.single().artists.single())
    }

    @Test
    fun incompletePlaylistsAreSkippedAndMissingCoverIsAllowed() = runBlocking {
        val result = source(
            own = """[{"kind":8,"title":"Без обложки","cover":{"uri":null}}, {"kind":null,"title":"Нет ID"}]""",
            saved = """[{"playlist":{"kind":9,"title":"Нет владельца"}}, {"playlist":{"kind":10,"uid":99,"title":null}}]""",
        ).getCollection()
        assertEquals(listOf("liked-tracks:42", "playlist:42:8"), result.map { it.id })
        assertNull(result.last().coverUrl)
    }

    @Test
    fun failedSectionFailsRefreshInsteadOfReturningPartialCollection() = runBlocking {
        val source = YandexMusicRemoteDataSource(tokens) { url, _ ->
            if (url.endsWith("/account/status")) JSONObject("""{"result":{"account":{"uid":42}}}""")
            else throw IllegalStateException("Service unavailable")
        }
        try {
            source.getCollection()
            fail("Expected request failure")
        } catch (expected: IllegalStateException) {
            assertEquals("Service unavailable", expected.message)
        }
    }
}

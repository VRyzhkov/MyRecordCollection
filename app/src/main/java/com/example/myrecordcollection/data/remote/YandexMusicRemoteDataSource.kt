package com.example.myrecordcollection.data.remote

import com.example.myrecordcollection.data.auth.TokenStorage
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.CollectionGroups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YandexMusicRemoteDataSource(
    private val tokenStorage: TokenStorage,
    private val requestJson: ((String, String) -> JSONObject)? = null,
) : MusicRemoteDataSource {
    override suspend fun getCollection(): List<Album> = withContext(Dispatchers.IO) {
        val token = tokenStorage.getAccessToken()
            ?: throw InvalidMusicTokenException("Требуется повторное подключение аккаунта")
        val status = getJson("$BASE_URL/account/status", token)
        val account = status.getJSONObject("result").getJSONObject("account")
        val userId = requireNotNull(account.text("uid")) { "В ответе Яндекс Музыки нет ID аккаунта" }
        val ownPlaylists = getJson("$BASE_URL/users/$userId/playlists/list", token).getJSONArray("result")
        val savedPlaylists = getJson("$BASE_URL/users/$userId/likes/playlists", token).getJSONArray("result")
        val response = getJson("$BASE_URL/users/$userId/likes/albums?rich=true", token)
        val result = response.getJSONArray("result")

        buildList {
            // One link to the entire liked-tracks collection, not albums extracted from its tracks.
            add(Album(
                id = "liked-tracks:$userId",
                title = "Мне нравится",
                artists = listOf(CollectionGroups.likedTracks),
                albumUrl = "https://music.yandex.ru/collection/tracks",
            ))
            for (index in 0 until ownPlaylists.length()) {
                val playlist = ownPlaylists.optJSONObject(index) ?: continue
                parsePlaylist(playlist, userId, defaultOwnerId = userId)?.let(::add)
            }
            for (index in 0 until savedPlaylists.length()) {
                val like = savedPlaylists.optJSONObject(index) ?: continue
                val playlist = like.optJSONObject("playlist") ?: like
                parsePlaylist(playlist, userId)?.let(::add)
            }
            for (index in 0 until result.length()) {
                val like = result.getJSONObject(index)
                val albumJson = like.optJSONObject("album") ?: continue
                parseAlbum(albumJson)?.let(::add)
            }
        }.distinctBy { it.id }
    }

    private fun getJson(url: String, token: String): JSONObject {
        requestJson?.let { return it(url, token) }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("Authorization", "OAuth $token")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "ru")
            setRequestProperty("X-Yandex-Music-Client", "MyRecordCollection-Android")
        }
        return try {
            val statusCode = connection.responseCode
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                statusCode == HttpURLConnection.HTTP_FORBIDDEN
            ) {
                throw InvalidMusicTokenException("Токен Яндекс Музыки недействителен")
            }
            if (statusCode !in 200..299) {
                throw IllegalStateException("Яндекс Музыка вернула HTTP $statusCode")
            }
            connection.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePlaylist(json: JSONObject, currentUserId: String, defaultOwnerId: String? = null): Album? {
        val owner = json.optJSONObject("owner")
        val ownerId = owner?.text("uid") ?: json.text("uid") ?: defaultOwnerId ?: return null
        val kind = json.text("kind") ?: return null
        // Yandex's own liked-tracks playlist is represented by our pinned card.
        if (ownerId == currentUserId && kind == "3") return null
        val title = json.text("title") ?: return null
        val cover = json.optJSONObject("cover")
        val coverUri = cover?.text("uri")
            ?: cover?.optJSONArray("itemsUri")?.optString(0)?.takeIf { it.isNotBlank() && it != "null" }
            ?: json.text("ogImage")
        val uuid = json.text("playlistUuid")
        val ownerLink = owner?.text("login") ?: ownerId
        val url = if (uuid != null) "https://music.yandex.ru/playlists/${uuid.urlSegment()}"
            else "https://music.yandex.ru/users/${ownerLink.urlSegment()}/playlists/${kind.urlSegment()}"
        return Album(
            id = "playlist:$ownerId:$kind",
            title = title,
            artists = listOf(CollectionGroups.playlists),
            coverUrl = coverUri?.toCoverUrl(),
            albumUrl = url,
        )
    }

    private fun parseAlbum(json: JSONObject): Album? {
        val id = json.text("id") ?: return null
        val title = json.text("title") ?: return null
        val artistsJson = json.optJSONArray("artists") ?: JSONArray()
        val artists = buildList {
            for (index in 0 until artistsJson.length()) {
                val artistJson = artistsJson.optJSONObject(index) ?: continue
                val artistId = artistJson.opt("id")?.toString() ?: continue
                val name = artistJson.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(Artist(id = artistId, name = name))
            }
        }
        if (artists.isEmpty()) return null

        val coverUrl = json.text("coverUri")?.toCoverUrl()

        return Album(
            id = id,
            title = title,
            artists = artists,
            coverUrl = coverUrl,
            albumUrl = "https://music.yandex.ru/album/$id",
        )
    }

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun String.urlSegment(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    private fun String.toCoverUrl(): String {
        val uri = replace("%%", "600x600")
        return when {
            uri.startsWith("//") -> "https:$uri"
            uri.startsWith("http://") || uri.startsWith("https://") -> uri
            else -> "https://$uri"
        }
    }

    private companion object {
        const val BASE_URL = "https://api.music.yandex.net"
        const val TIMEOUT_MILLIS = 15_000
    }
}

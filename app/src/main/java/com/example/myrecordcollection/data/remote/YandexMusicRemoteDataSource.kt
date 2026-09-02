package com.example.myrecordcollection.data.remote

import com.example.myrecordcollection.data.auth.TokenStorage
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class YandexMusicRemoteDataSource(
    private val tokenStorage: TokenStorage,
) : MusicRemoteDataSource {
    override suspend fun getFavoriteAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val token = tokenStorage.getAccessToken()
            ?: throw InvalidMusicTokenException("Требуется повторное подключение аккаунта")
        val status = getJson("$BASE_URL/account/status", token)
        val account = status.getJSONObject("result").getJSONObject("account")
        val userId = account.get("uid").toString()
        val response = getJson("$BASE_URL/users/$userId/likes/albums?rich=true", token)
        val result = response.getJSONArray("result")

        buildList {
            for (index in 0 until result.length()) {
                val like = result.getJSONObject(index)
                val albumJson = like.optJSONObject("album") ?: continue
                parseAlbum(albumJson)?.let(::add)
            }
        }
    }

    private fun getJson(url: String, token: String): JSONObject {
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

    private fun parseAlbum(json: JSONObject): Album? {
        val id = json.opt("id")?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
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

        val coverUrl = json.optString("coverUri")
            .takeIf { it.isNotBlank() }
            ?.replace("%%", "600x600")
            ?.let { uri ->
                when {
                    uri.startsWith("//") -> "https:$uri"
                    uri.startsWith("http://") || uri.startsWith("https://") -> uri
                    else -> "https://$uri"
                }
            }

        return Album(
            id = id,
            title = title,
            artists = artists,
            coverUrl = coverUrl,
            albumUrl = "https://music.yandex.ru/album/$id",
        )
    }

    private companion object {
        const val BASE_URL = "https://api.music.yandex.net"
        const val TIMEOUT_MILLIS = 15_000
    }
}

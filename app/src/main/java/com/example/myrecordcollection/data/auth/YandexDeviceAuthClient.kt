package com.example.myrecordcollection.data.auth

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class DeviceAuthorizationCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Long,
    val pollingIntervalSeconds: Long,
)

data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String?,
)

class YandexDeviceAuthClient {
    suspend fun requestCode(): DeviceAuthorizationCode = withContext(Dispatchers.IO) {
        val response = postForm(
            url = "$OAUTH_BASE_URL/device/code",
            parameters = mapOf(
                "client_id" to CLIENT_ID,
                "device_id" to UUID.randomUUID().toString(),
                "device_name" to "MyRecordCollection ${Build.MODEL}",
            ),
        )
        DeviceAuthorizationCode(
            deviceCode = response.getString("device_code"),
            userCode = response.getString("user_code"),
            verificationUrl = response.getString("verification_url"),
            expiresInSeconds = response.optLong("expires_in", 600L),
            pollingIntervalSeconds = response.optLong("interval", 5L).coerceAtLeast(1L),
        )
    }

    suspend fun waitForTokens(code: DeviceAuthorizationCode): OAuthTokens {
        val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1_000L
        while (System.currentTimeMillis() < deadline) {
            when (val result = pollToken(code.deviceCode)) {
                is PollResult.Success -> return result.tokens
                PollResult.Pending,
                PollResult.NetworkUnavailable,
                -> delay(code.pollingIntervalSeconds * 1_000L)
            }
        }
        throw IllegalStateException("Время подтверждения входа истекло")
    }

    private suspend fun pollToken(deviceCode: String): PollResult = withContext(Dispatchers.IO) {
        try {
            val response = postForm(
                url = "$OAUTH_BASE_URL/token",
                parameters = mapOf(
                    "grant_type" to "device_code",
                    "code" to deviceCode,
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET,
                ),
            )
            PollResult.Success(
                OAuthTokens(
                    accessToken = response.getString("access_token"),
                    refreshToken = response.optString("refresh_token").takeIf { it.isNotBlank() },
                ),
            )
        } catch (error: OAuthException) {
            if (error.code == "authorization_pending") PollResult.Pending else throw error
        } catch (_: IOException) {
            PollResult.NetworkUnavailable
        }
    }

    private fun postForm(url: String, parameters: Map<String, String>): JSONObject {
        val body = parameters.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }.toByteArray(Charsets.UTF_8)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setFixedLengthStreamingMode(body.size)
        }
        return try {
            connection.outputStream.use { it.write(body) }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val json = stream.bufferedReader().use { JSONObject(it.readText()) }
            if (connection.responseCode !in 200..299) {
                throw OAuthException(
                    code = json.optString("error", "oauth_error"),
                    message = json.optString("error_description", "Ошибка авторизации Яндекса"),
                )
            }
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private sealed interface PollResult {
        data object Pending : PollResult
        data object NetworkUnavailable : PollResult
        data class Success(val tokens: OAuthTokens) : PollResult
    }

    private class OAuthException(val code: String, message: String) : Exception(message)

    private companion object {
        const val OAUTH_BASE_URL = "https://oauth.yandex.ru"
        const val TIMEOUT_MILLIS = 15_000

        // Public OAuth credentials of the official Yandex Music Android client.
        const val CLIENT_ID = "23cabbbdc6cd418abb4b39c32c41195d"
        const val CLIENT_SECRET = "53bc75238f0c4d08a118e51fe9203300"
    }
}

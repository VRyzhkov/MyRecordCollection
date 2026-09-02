package com.example.myrecordcollection.data.cover

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class AlbumCoverStorage(context: Context) {
    private val coverDirectory = File(context.filesDir, COVER_DIRECTORY)

    suspend fun save(albumId: String, remoteUrl: String): String = withContext(Dispatchers.IO) {
        coverDirectory.mkdirs()
        val destination = File(coverDirectory, "${albumId.sha256()}.image")
        if (destination.isFile && destination.length() > 0L) {
            return@withContext destination.absolutePath
        }

        val temporary = File(coverDirectory, "${destination.name}.download")
        temporary.delete()
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.connect()
            check(connection.responseCode in 200..299) {
                "Не удалось загрузить обложку: HTTP ${connection.responseCode}"
            }
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            moveReplacing(temporary, destination)
            destination.absolutePath
        } finally {
            connection.disconnect()
            temporary.delete()
        }
    }

    suspend fun removeUnused(usedPaths: Set<String>) = withContext(Dispatchers.IO) {
        val usedNames = usedPaths.mapTo(mutableSetOf()) { File(it).name }
        coverDirectory.listFiles()?.forEach { file ->
            if (file.name.endsWith(".download") || file.name !in usedNames) {
                file.delete()
            }
        }
    }

    private fun moveReplacing(source: File, destination: File) {
        runCatching {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val COVER_DIRECTORY = "album_covers"
    }
}

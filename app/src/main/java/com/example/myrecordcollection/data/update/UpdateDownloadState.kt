package com.example.myrecordcollection.data.update

import android.app.DownloadManager

enum class UpdatePhase { Hidden, Offer, Downloading, Ready, Failed }

data class UpdateDownloadState(
    val phase: UpdatePhase = UpdatePhase.Hidden,
    val update: AvailableUpdate? = null,
    val message: String = "",
)

internal fun downloadStatus(status: Int, downloaded: Long, total: Long, reason: Int): Pair<UpdatePhase, String> =
    when (status) {
        DownloadManager.STATUS_SUCCESSFUL -> UpdatePhase.Ready to "APK скачан. Нажмите «Установить»."
        DownloadManager.STATUS_FAILED -> UpdatePhase.Failed to "Не удалось скачать обновление (код $reason). Попробуйте ещё раз."
        DownloadManager.STATUS_PAUSED -> UpdatePhase.Downloading to "Загрузка приостановлена. Ожидание сети или повторной попытки (код $reason)."
        DownloadManager.STATUS_PENDING -> UpdatePhase.Downloading to "Ожидание начала загрузки…"
        DownloadManager.STATUS_RUNNING -> UpdatePhase.Downloading to if (total > 0) {
            val percent = (downloaded.toDouble() / total * 100).toInt().coerceIn(0, 100)
            if (percent == 100) "Файл получен. Android завершает загрузку…" else "Загрузка обновления: $percent%"
        } else "Загрузка обновления…"
        else -> UpdatePhase.Failed to "Не удалось определить состояние загрузки. Попробуйте ещё раз."
    }

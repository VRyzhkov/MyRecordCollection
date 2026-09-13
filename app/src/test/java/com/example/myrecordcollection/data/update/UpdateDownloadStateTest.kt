package com.example.myrecordcollection.data.update

import android.app.DownloadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDownloadStateTest {
    @Test
    fun receivingAllBytesDoesNotMeanAndroidHasCompletedDownload() {
        val result = downloadStatus(DownloadManager.STATUS_RUNNING, 100, 100, 0)
        assertEquals(UpdatePhase.Downloading, result.first)
        assertTrue(result.second.contains("завершает"))
    }

    @Test
    fun successfulDownloadOffersInstallationEvenWhenLengthIsUnknown() {
        assertEquals(UpdatePhase.Ready, downloadStatus(DownloadManager.STATUS_SUCCESSFUL, 100, -1, 0).first)
    }

    @Test
    fun pausedDownloadExplainsWaitingAndFailureAllowsRetry() {
        assertTrue(downloadStatus(DownloadManager.STATUS_PAUSED, 10, 100, 2).second.contains("Ожидание"))
        val failure = downloadStatus(DownloadManager.STATUS_FAILED, 100, 100, 1006)
        assertEquals(UpdatePhase.Failed, failure.first)
        assertTrue(failure.second.contains("1006"))
    }

    @Test
    fun unknownLengthAndStatusAreHandled() {
        assertEquals(UpdatePhase.Downloading, downloadStatus(DownloadManager.STATUS_RUNNING, 10, -1, 0).first)
        assertEquals(UpdatePhase.Failed, downloadStatus(0, 0, 0, 0).first)
    }
}

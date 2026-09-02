package com.example.myrecordcollection.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedTokenStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = EncryptedTokenStorage(
        context = context,
        preferencesName = "secure_auth_test",
        keyAlias = "my_record_collection_access_token_test",
    )

    @Test
    fun tokenCanBeEncryptedReadAndCleared() {
        storage.clear()

        storage.saveAccessToken("secret-token")
        assertEquals("secret-token", storage.getAccessToken())

        storage.clear()
        assertNull(storage.getAccessToken())
    }
}

package com.example.myrecordcollection.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedTokenStorage(
    context: Context,
    preferencesName: String = PREFERENCES_NAME,
    private val keyAlias: String = KEY_ALIAS,
) : TokenStorage {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun getAccessToken(): String? {
        val encrypted = preferences.getString(ACCESS_TOKEN_KEY, null) ?: return null
        return runCatching { decrypt(encrypted) }.getOrNull()
    }

    override fun saveAccessToken(token: String) {
        require(token.isNotBlank())
        preferences.edit().putString(ACCESS_TOKEN_KEY, encrypt(token.trim())).apply()
    }

    override fun clear() {
        preferences.edit().remove(ACCESS_TOKEN_KEY).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(cipher.iv, encrypted).joinToString(SEPARATOR) {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
    }

    private fun decrypt(value: String): String {
        val parts = value.split(SEPARATOR, limit = 2)
        require(parts.size == 2)
        val initializationVector = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, initializationVector),
        )
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_NAME).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "secure_auth"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val KEY_STORE_NAME = "AndroidKeyStore"
        const val KEY_ALIAS = "my_record_collection_access_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "."
    }
}

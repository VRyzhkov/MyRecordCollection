package com.example.myrecordcollection.data.auth

interface TokenStorage {
    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun saveTokens(accessToken: String, refreshToken: String?)

    fun clear()
}

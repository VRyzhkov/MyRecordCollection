package com.example.myrecordcollection.data.auth

interface TokenStorage {
    fun getAccessToken(): String?

    fun saveAccessToken(token: String)

    fun clear()
}

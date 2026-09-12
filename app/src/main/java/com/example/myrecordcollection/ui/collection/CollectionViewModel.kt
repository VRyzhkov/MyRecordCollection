package com.example.myrecordcollection.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecordcollection.app.MyRecordCollectionApp
import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.ArtistGroup
import com.example.myrecordcollection.data.remote.InvalidMusicTokenException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val musicRepository: MusicRepository =
        (application as MyRecordCollectionApp).musicRepository
    private val tokenStorage = (application as MyRecordCollectionApp).tokenStorage
    private val deviceAuthClient = (application as MyRecordCollectionApp).deviceAuthClient

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.CheckingAuth)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var groups: List<ArtistGroup> = emptyList()
    private var isRefreshing = false
    private var collectionJob: Job? = null
    private var authorizationJob: Job? = null
    private var collectionEnabled = false

    init {
        checkAuthorization()
    }

    fun startYandexAuthorization() {
        if (_uiState.value is CollectionUiState.Authorizing) return
        _uiState.value = CollectionUiState.Authorizing()
        authorizationJob = viewModelScope.launch {
            try {
                val code = deviceAuthClient.requestCode()
                _uiState.value = CollectionUiState.Authorizing(
                    userCode = code.userCode,
                    verificationUrl = code.verificationUrl,
                )
                val tokens = deviceAuthClient.waitForTokens(code)
                tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                startCollection()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = CollectionUiState.SignedOut(
                    error.message ?: "Не удалось выполнить вход через Яндекс",
                )
            }
        }
    }

    fun cancelAuthorization() {
        if (_uiState.value is CollectionUiState.Authorizing) {
            authorizationJob?.cancel()
            authorizationJob = null
            _uiState.value = CollectionUiState.SignedOut()
        }
    }

    fun signOut() {
        authorizationJob?.cancel()
        tokenStorage.clear()
        collectionEnabled = false
        collectionJob?.cancel()
        groups = emptyList()
        isRefreshing = false
        _uiState.value = CollectionUiState.SignedOut()
    }

    fun loadCollection() {
        if (!collectionEnabled || isRefreshing) return
        isRefreshing = true
        publishState()
        refreshCollection()
    }

    private fun checkAuthorization() {
        viewModelScope.launch {
            val hasToken = tokenStorage.getAccessToken() != null
            if (hasToken) startCollection() else {
                _uiState.value = CollectionUiState.SignedOut()
            }
        }
    }

    private fun startCollection() {
        if (collectionEnabled) return
        collectionEnabled = true
        isRefreshing = true
        _uiState.value = CollectionUiState.Loading
        collectionJob = viewModelScope.launch {
            musicRepository.observeAlbumGroups().collect { storedGroups ->
                groups = storedGroups
                publishState()
            }
        }
        refreshCollection()
    }

    private fun refreshCollection() {
        viewModelScope.launch {
            try {
                musicRepository.refreshCollection()
                isRefreshing = false
                publishState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: InvalidMusicTokenException) {
                tokenStorage.clear()
                collectionEnabled = false
                collectionJob?.cancel()
                groups = emptyList()
                isRefreshing = false
                _uiState.value = CollectionUiState.SignedOut(error.message)
            } catch (error: Exception) {
                isRefreshing = false
                val message = error.message ?: "Не удалось обновить коллекцию"
                _uiState.value = if (groups.isEmpty()) {
                    CollectionUiState.Error(message)
                } else {
                    CollectionUiState.Content(groups, refreshError = message)
                }
            }
        }
    }

    private fun publishState() {
        if (!collectionEnabled) return
        _uiState.value = when {
            groups.isNotEmpty() -> CollectionUiState.Content(groups, isRefreshing)
            isRefreshing -> CollectionUiState.Loading
            else -> CollectionUiState.Empty
        }
    }

}

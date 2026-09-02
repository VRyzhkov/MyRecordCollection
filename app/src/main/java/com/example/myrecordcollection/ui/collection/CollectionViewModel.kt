package com.example.myrecordcollection.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecordcollection.app.MyRecordCollectionApp
import com.example.myrecordcollection.data.music.MusicRepository
import com.example.myrecordcollection.domain.model.ArtistGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val musicRepository: MusicRepository =
        (application as MyRecordCollectionApp).musicRepository

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var groups: List<ArtistGroup> = emptyList()
    private var isRefreshing = true

    init {
        observeCollection()
        refreshCollection()
    }

    fun loadCollection() {
        if (isRefreshing) return
        isRefreshing = true
        publishState()
        refreshCollection()
    }

    private fun observeCollection() {
        viewModelScope.launch {
            musicRepository.observeAlbumGroups().collect { storedGroups ->
                groups = storedGroups
                publishState()
            }
        }
    }

    private fun refreshCollection() {
        viewModelScope.launch {
            try {
                musicRepository.refreshCollection()
                isRefreshing = false
                publishState()
            } catch (error: CancellationException) {
                throw error
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
        _uiState.value = when {
            groups.isNotEmpty() -> CollectionUiState.Content(groups, isRefreshing)
            isRefreshing -> CollectionUiState.Loading
            else -> CollectionUiState.Empty
        }
    }
}

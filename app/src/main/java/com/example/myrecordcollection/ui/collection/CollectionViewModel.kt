package com.example.myrecordcollection.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myrecordcollection.data.music.FakeMusicRepository
import com.example.myrecordcollection.domain.usecase.LoadAlbumGroupsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val loadAlbumGroups: LoadAlbumGroupsUseCase = LoadAlbumGroupsUseCase(
        musicRepository = FakeMusicRepository(),
    ),
) : ViewModel() {
    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
    }

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.value = CollectionUiState.Loading

            try {
                val groups = loadAlbumGroups()
                _uiState.value = if (groups.isEmpty()) {
                    CollectionUiState.Empty
                } else {
                    CollectionUiState.Content(groups)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = CollectionUiState.Error(
                    message = error.message ?: "Не удалось загрузить коллекцию",
                )
            }
        }
    }
}

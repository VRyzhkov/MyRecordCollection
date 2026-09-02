package com.example.myrecordcollection.ui.collection

import com.example.myrecordcollection.domain.model.ArtistGroup

sealed interface CollectionUiState {
    data object CheckingAuth : CollectionUiState

    data class SignedOut(
        val message: String? = null,
    ) : CollectionUiState

    data object Loading : CollectionUiState

    data object Empty : CollectionUiState

    data class Content(
        val groups: List<ArtistGroup>,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : CollectionUiState

    data class Error(
        val message: String,
    ) : CollectionUiState
}

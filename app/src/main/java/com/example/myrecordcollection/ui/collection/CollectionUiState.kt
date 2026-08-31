package com.example.myrecordcollection.ui.collection

import com.example.myrecordcollection.domain.model.ArtistGroup

sealed interface CollectionUiState {
    data object Loading : CollectionUiState

    data object Empty : CollectionUiState

    data class Content(
        val groups: List<ArtistGroup>,
    ) : CollectionUiState

    data class Error(
        val message: String,
    ) : CollectionUiState
}

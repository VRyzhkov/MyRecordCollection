package com.example.myrecordcollection.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.ArtistGroup
import com.example.myrecordcollection.ui.theme.MyRecordCollectionTheme

@Composable
fun CollectionRoute(
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectionScreen(
        uiState = uiState,
        onRetry = viewModel::loadCollection,
        onRefresh = viewModel::loadCollection,
        modifier = modifier,
    )
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = onRetry,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ),
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when (uiState) {
                CollectionUiState.Loading -> LoadingContent()
                CollectionUiState.Empty -> EmptyContent()
                is CollectionUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                )
                is CollectionUiState.Content -> CollectionContent(
                    state = uiState,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    MessageContent(
        title = "Коллекция пуста",
        description = "Добавьте музыку в избранное Яндекс Музыки.",
    )
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Не удалось загрузить коллекцию",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun MessageContent(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionContent(
    state: CollectionUiState.Content,
    onRefresh: () -> Unit,
) {
    val albums = state.groups.flatMap { group -> group.albums }
    var centeredAlbum by remember(albums) { mutableStateOf(albums.firstOrNull()) }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = centeredAlbum?.artists?.firstOrNull()?.name ?: "Моя коллекция",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = centeredAlbum?.title ?: "Альбомы сгруппированы по исполнителям",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
                state.refreshError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            AlbumCarousel(
                albums = albums,
                onCenteredAlbumChanged = { album -> centeredAlbum = album },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CollectionScreenPreview() {
    val artist = Artist(id = "preview-artist", name = "Pink Floyd")
    val albums = listOf(
        Album(id = "preview-dark-side", title = "The Dark Side of the Moon", artists = listOf(artist)),
        Album(id = "preview-wish", title = "Wish You Were Here", artists = listOf(artist)),
    )

    MyRecordCollectionTheme(dynamicColor = false) {
        CollectionScreen(
            uiState = CollectionUiState.Content(
                groups = listOf(ArtistGroup(artist = artist, albums = albums)),
            ),
            onRetry = {},
        )
    }
}

package com.example.myrecordcollection.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        modifier = modifier,
    )
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState,
    onRetry: () -> Unit,
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
                is CollectionUiState.Content -> CollectionContent(groups = uiState.groups)
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

@Composable
private fun CollectionContent(groups: List<ArtistGroup>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Моя коллекция",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Первый прототип на тестовых данных",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        items(
            items = groups,
            key = { group -> group.artist.id },
        ) { group ->
            ArtistAlbumGroup(group = group)
        }
    }
}

@Composable
private fun ArtistAlbumGroup(group: ArtistGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = group.artist.name,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = group.albums,
                key = { album -> album.id },
            ) { album ->
                AlbumItem(album = album)
            }
        }
    }
}

@Composable
private fun AlbumItem(album: Album) {
    Column(
        modifier = Modifier.size(width = 136.dp, height = 172.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(albumColor(album.id)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = album.title.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun albumColor(id: String): Color {
    val colors = listOf(
        Color(0xFF6D4C9F),
        Color(0xFF006D77),
        Color(0xFFB24C63),
        Color(0xFF3A5A40),
        Color(0xFFBC6C25),
    )
    return colors[(id.hashCode() and Int.MAX_VALUE) % colors.size]
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

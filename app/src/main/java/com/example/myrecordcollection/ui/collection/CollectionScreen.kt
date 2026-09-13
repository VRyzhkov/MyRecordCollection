package com.example.myrecordcollection.ui.collection

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myrecordcollection.domain.model.Album
import com.example.myrecordcollection.domain.model.Artist
import com.example.myrecordcollection.domain.model.ArtistGroup
import com.example.myrecordcollection.input.SteeringCommand
import com.example.myrecordcollection.input.SteeringWheelController
import com.example.myrecordcollection.ui.theme.MyRecordCollectionTheme
import com.example.myrecordcollection.ui.theme.ThemeMode
import kotlinx.coroutines.delay

@Composable
fun CollectionRoute(
    steeringWheelController: SteeringWheelController,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectionScreen(
        uiState = uiState,
        onRetry = viewModel::loadCollection,
        onRefresh = viewModel::loadCollection,
        onStartAuthorization = viewModel::startYandexAuthorization,
        onCancelAuthorization = viewModel::cancelAuthorization,
        onSignOut = viewModel::signOut,
        steeringWheelController = steeringWheelController,
        themeMode = themeMode,
        onThemeModeChanged = onThemeModeChanged,
        modifier = modifier,
    )
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = onRetry,
    onStartAuthorization: () -> Unit = {},
    onCancelAuthorization: () -> Unit = {},
    onSignOut: () -> Unit = {},
    steeringWheelController: SteeringWheelController? = null,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
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
                CollectionUiState.CheckingAuth -> LoadingContent()
                CollectionUiState.Loading -> LoadingContent()
                is CollectionUiState.SignedOut -> SignInContent(
                    message = uiState.message,
                    onStartAuthorization = onStartAuthorization,
                )
                is CollectionUiState.Authorizing -> AuthorizingContent(
                    state = uiState,
                    onCancel = onCancelAuthorization,
                )
                CollectionUiState.Empty -> EmptyContent()
                is CollectionUiState.Error -> ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                )
                is CollectionUiState.Content -> CollectionContent(
                    state = uiState,
                    onRefresh = onRefresh,
                    steeringWheelController = steeringWheelController,
                )
            }
            SettingsMenu(
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
                collectionActionsEnabled = uiState is CollectionUiState.Content,
                onRefresh = onRefresh,
                onSignOut = onSignOut,
                steeringWheelController = steeringWheelController,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun SettingsMenu(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    collectionActionsEnabled: Boolean,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    steeringWheelController: SteeringWheelController?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showSteeringSettings by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Настройки" },
        ) {
            Text(text = "⋮", style = MaterialTheme.typography.headlineMedium)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (collectionActionsEnabled) {
                if (steeringWheelController != null) {
                    DropdownMenuItem(
                        text = { Text("Настройка кнопок руля") },
                        onClick = {
                            expanded = false
                            showSteeringSettings = true
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Обновить коллекцию") },
                    onClick = {
                        onRefresh()
                        expanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Отключить аккаунт") },
                    onClick = {
                        onSignOut()
                        expanded = false
                    },
                )
            }
            Text(
                text = "Тема оформления",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.title) },
                    leadingIcon = {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = null,
                        )
                    },
                    onClick = {
                        onThemeModeChanged(mode)
                        expanded = false
                    },
                )
            }
        }
    }
    if (showSteeringSettings && steeringWheelController != null) {
        SteeringSettingsDialog(
            controller = steeringWheelController,
            onDismiss = {
                steeringWheelController.cancelLearning()
                showSteeringSettings = false
            },
        )
    }
}

@Composable
private fun SteeringSettingsDialog(
    controller: SteeringWheelController,
    onDismiss: () -> Unit,
) {
    val mappings by controller.mappings.collectAsStateWithLifecycle()
    val pendingCommand by controller.pendingCommand.collectAsStateWithLifecycle()

    LaunchedEffect(pendingCommand) {
        if (pendingCommand != null) {
            delay(10_000)
            controller.cancelLearning()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Кнопки руля") },
        text = {
            Column {
                Text(
                    text = pendingCommand?.let {
                        "Нажмите кнопку для команды «${it.title}»"
                    } ?: "Выберите команду, затем нажмите нужную кнопку на руле.",
                    color = if (pendingCommand != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                SteeringCommand.entries.forEach { command ->
                    val keyCode = mappings[command]
                    TextButton(
                        onClick = { controller.startLearning(command) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(command.title)
                            Text(
                                text = keyCode?.let(::keyCodeTitle) ?: "Не назначено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = controller::clearAllMappings) { Text("Очистить") }
                TextButton(onClick = controller::resetDefaults) { Text("По умолчанию") }
            }
        },
    )
}

private fun keyCodeTitle(keyCode: Int): String =
    "${KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")} ($keyCode)"

@Composable
private fun SignInContent(
    message: String?,
    onStartAuthorization: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Подключение Яндекс Музыки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Вход выполняется на сайте Яндекса. Пароль не передаётся приложению, а полученные токены хранятся через Android Keystore.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartAuthorization,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Войти через Яндекс")
        }
        Text(
            text = "Используется неофициальный API Яндекс Музыки; его контракт может измениться.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AuthorizingContent(
    state: CollectionUiState.Authorizing,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val openAuthorizationSite: () -> Unit = {
        val url = state.verificationUrl
        if (url != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    LaunchedEffect(state.verificationUrl) {
        if (state.verificationUrl != null) openAuthorizationSite()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Подтвердите вход",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.userCode == null) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Получаем одноразовый код…")
        } else {
            Text("Введите на сайте Яндекса код")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.userCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = openAuthorizationSite,
                enabled = state.verificationUrl != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Открыть сайт Яндекса")
            }
            Text(
                text = "После подтверждения вернитесь в приложение — вход завершится автоматически.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = onCancel) {
            Text("Отмена")
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Загрузка может занять продолжительное время",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
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
    steeringWheelController: SteeringWheelController?,
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val albums = state.groups.flatMap { group -> group.albums }
    var centeredAlbum by remember(albums) { mutableStateOf(albums.firstOrNull()) }
    var requestedIndex by remember(albums) { mutableStateOf<Int?>(null) }
    var navigationRequestId by remember(albums) { mutableIntStateOf(0) }
    val currentCenteredAlbum by rememberUpdatedState(centeredAlbum)

    DisposableEffect(steeringWheelController) {
        steeringWheelController?.setEnabled(true)
        onDispose { steeringWheelController?.setEnabled(false) }
    }

    fun openAlbum(album: Album?) {
        album?.albumUrl?.let { url ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    LaunchedEffect(steeringWheelController, albums, state.groups) {
        steeringWheelController?.commands?.collect { command ->
            val currentIndex = currentCenteredAlbum?.let { current ->
                albums.indexOfFirst { it.id == current.id }
            }?.takeIf { it >= 0 } ?: 0
            val targetIndex = when (command) {
                SteeringCommand.NextAlbum -> (currentIndex + 1).coerceAtMost(albums.lastIndex)
                SteeringCommand.PreviousAlbum -> (currentIndex - 1).coerceAtLeast(0)
                SteeringCommand.NextArtist -> state.groups
                    .dropWhile { group -> group.albums.none { it.id == albums[currentIndex].id } }
                    .drop(1)
                    .firstOrNull()
                    ?.albums
                    ?.firstOrNull()
                    ?.let { album -> albums.indexOfFirst { it.id == album.id } }
                    ?: currentIndex
                SteeringCommand.PreviousArtist -> {
                    val groupIndex = state.groups.indexOfFirst { group ->
                        group.albums.any { it.id == albums[currentIndex].id }
                    }
                    state.groups.getOrNull(groupIndex - 1)
                        ?.albums
                        ?.firstOrNull()
                        ?.let { album -> albums.indexOfFirst { it.id == album.id } }
                        ?: currentIndex
                }
                SteeringCommand.PlayAlbum -> {
                    openAlbum(currentCenteredAlbum)
                    currentIndex
                }
            }
            if (command != SteeringCommand.PlayAlbum) {
                requestedIndex = targetIndex
                navigationRequestId++
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = if (isLandscape) 12.dp else 24.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                AnimatedContent(
                    targetState = centeredAlbum,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "centeredAlbumTitle",
                ) { album ->
                    Column {
                        Text(
                            text = album?.artists?.firstOrNull()?.name ?: "Моя коллекция",
                            style = if (isLandscape) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.headlineLarge
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = album?.title ?: "Альбомы сгруппированы по исполнителям",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
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
                requestedIndex = requestedIndex,
                navigationRequestId = navigationRequestId,
                onCenteredAlbumChanged = { album ->
                    centeredAlbum = album
                },
                onCenteredAlbumClick = ::openAlbum,
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

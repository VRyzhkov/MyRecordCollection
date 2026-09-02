package com.example.myrecordcollection.ui.collection

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.example.myrecordcollection.domain.model.Album
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AlbumCarousel(
    albums: List<Album>,
    modifier: Modifier = Modifier,
    onCenteredAlbumChanged: (Album) -> Unit = {},
) {
    if (albums.isEmpty()) return

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxPosition = albums.lastIndex.toFloat()
    var position by remember(albums) { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val centeredIndex = position.roundToInt().coerceIn(albums.indices)

    LaunchedEffect(albums, centeredIndex) {
        onCenteredAlbumChanged(albums[centeredIndex])
    }

    BoxWithConstraints(modifier = modifier) {
        val dragStepPx = with(density) {
            (if (isLandscape) maxWidth * 0.24f else maxHeight * 0.20f).toPx()
        }.coerceAtLeast(1f)
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val dragState = rememberDraggableState { delta ->
            position = (position - delta / dragStepPx).coerceIn(0f, maxPosition)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = dragState,
                    orientation = if (isLandscape) Orientation.Horizontal else Orientation.Vertical,
                    onDragStarted = { settleJob?.cancel() },
                    onDragStopped = { velocity ->
                        settleJob = scope.launch {
                            val animatedPosition = Animatable(position)
                            animatedPosition.updateBounds(0f, maxPosition)
                            animatedPosition.animateDecay(
                                initialVelocity = -velocity / dragStepPx,
                                animationSpec = exponentialDecay(frictionMultiplier = 2.4f),
                            ) { position = value }
                            animatedPosition.animateTo(
                                targetValue = animatedPosition.value.roundToInt().toFloat(),
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                            ) { position = value }
                        }
                    },
                ),
        ) {
            val itemSize = if (isLandscape) 124.dp else 112.dp
            val itemSizePx = with(density) { itemSize.toPx() }

            albums.forEachIndexed { index, album ->
                val distance = index - position
                val t = (0.5f + distance / 5f).coerceIn(0f, 1f)
                val centerProximity = (1f - abs(t - 0.5f) * 2f).coerceIn(0f, 1f)
                val scale = 0.58f + 0.82f * centerProximity * centerProximity
                val (x, y) = carouselPoint(t, widthPx, heightPx, isLandscape)

                AlbumCarouselItem(
                    album = album,
                    modifier = Modifier
                        .size(itemSize)
                        .zIndex(scale)
                        .graphicsLayer {
                            translationX = x - itemSizePx / 2f
                            translationY = y - itemSizePx / 2f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (abs(distance) <= 2.7f) {
                                0.48f + 0.52f * centerProximity
                            } else {
                                0f
                            }
                        },
                )
            }
        }
    }
}

private fun carouselPoint(
    t: Float,
    width: Float,
    height: Float,
    isLandscape: Boolean,
): Pair<Float, Float> {
    val start = if (isLandscape) 0.08f to 0.86f else 0.82f to 0.08f
    val control = if (isLandscape) 0.50f to 0.14f else 0.18f to 0.50f
    val end = if (isLandscape) 0.92f to 0.86f else 0.82f to 0.92f
    val inverse = 1f - t
    val x = inverse * inverse * start.first +
        2f * inverse * t * control.first +
        t * t * end.first
    val y = inverse * inverse * start.second +
        2f * inverse * t * control.second +
        t * t * end.second
    return x * width to y * height
}

@Composable
private fun AlbumCarouselItem(
    album: Album,
    modifier: Modifier = Modifier,
) {
    var imageLoaded by remember(album.localCoverPath) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(albumColor(album.id))
            .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageLoaded) {
            Text(
                text = album.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp),
            )
        }
        if (album.localCoverPath != null) {
            AsyncImage(
                model = album.localCoverPath,
                contentDescription = "Обложка альбома ${album.title}",
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoaded = true },
                onError = { imageLoaded = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
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

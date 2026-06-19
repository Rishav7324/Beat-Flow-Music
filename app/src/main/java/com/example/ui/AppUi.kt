package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.data.Song
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.MusicPlayerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PulseMusicApp(viewModel: MusicViewModel = viewModel()) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permission)
    
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.loadMusic()
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isAdExpanded by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!isPlayerExpanded && !isAdExpanded) {
                Column {
                    state.currentSong?.let { song ->
                        PremiumMiniPlayer(
                            song = song,
                            isPlaying = state.isPlaying,
                            onPlayPause = { viewModel.togglePlayback() },
                            onNext = { viewModel.skipNext() },
                            onClick = { isPlayerExpanded = true }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.9f),
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                        ) {
                            val tabs = listOf("Songs" to Icons.Rounded.MusicNote, "Folders" to Icons.Rounded.Folder, "EQ" to Icons.Rounded.Tune, "Ads" to Icons.Rounded.MonetizationOn)
                            tabs.forEachIndexed { index, pair ->
                                NavigationBarItem(
                                    icon = {
                                        AnimatedIcon(
                                            icon = pair.second,
                                            contentDescription = pair.first,
                                            selected = currentTab == index
                                        )
                                    },
                                    label = { Text(pair.first, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    selected = currentTab == index,
                                    onClick = { currentTab = index },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (permissionState.status.isGranted) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(modifier = Modifier.padding(padding)) {
                        TopBar(title = "Beat Flow Music")
                        when (currentTab) {
                            0 -> SongList(state, viewModel)
                            1 -> FolderList(state, viewModel)
                            2 -> EqualizerPlaceholder()
                            3 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Button(onClick = { isAdExpanded = true }) { Text("Play Ad Simulation") }
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Permission required to read music.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ExpandedPlayer(
                    state = state,
                    viewModel = viewModel,
                    onClose = { isPlayerExpanded = false }
                )
            }

            AnimatedVisibility(
                visible = isAdExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                AdsPlayNowScreen(onClose = { isAdExpanded = false })
            }
        }
    }
}

@Composable
fun TopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GOOD MORNING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, "Search", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, "More Options", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SongList(state: MusicPlayerState, viewModel: MusicViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 200.dp)
    ) {
        items(state.songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                isSelected = state.currentSong?.id == song.id,
                isPlaying = state.currentSong?.id == song.id && state.isPlaying,
                onClick = { viewModel.playSong(song) }
            )
        }
    }
}

@Composable
fun FolderList(state: MusicPlayerState, viewModel: MusicViewModel) {
    val folders = state.songs.groupBy { it.folderPath }.keys.toList().sorted()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        items(folders) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                    .clickable { /* Expand folder */ }
                    .padding(20.dp, 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = "Folder",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = folder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Rounded.ChevronRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EqualizerPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.2f), RoundedCornerShape(32.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "EQ", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AUDIO ENGINE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Equalizer Active",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lossless FLAC & DSP playback enabled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Premium Mock sliders
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(0.7f, 0.4f, 0.6f, 0.3f, 0.8f).forEach { initialValue ->
                    var value by remember { mutableStateOf(initialValue) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(value)
                                    .align(Alignment.BottomCenter)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, isSelected: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = isPlaying, label = "playPauseIcon") { playing ->
                if (playing) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Playing", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = "Song", tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumMiniPlayer(song: Song, isPlaying: Boolean, onPlayPause: () -> Unit, onNext: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Album Art", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedIconButton(
                    onClick = onPlayPause,
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedIconButton(
                    onClick = onNext,
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                )
            }
            // Real animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
fun ExpandedPlayer(state: MusicPlayerState, viewModel: MusicViewModel, onClose: () -> Unit) {
    val song = state.currentSong ?: return
    val context = LocalContext.current
    
    // Palette state
    var dominantColor by remember { mutableStateOf<Color?>(null) }
    var secondaryColor by remember { mutableStateOf<Color?>(null) }

    // Fetch color using Coil and Palette API
    LaunchedEffect(song) {
        try {
            val req = ImageRequest.Builder(context)
                .data(song.uri)
                .allowHardware(false)
                .size(200)
                .build()
            val result = context.imageLoader.execute(req)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                bitmap?.let {
                    androidx.palette.graphics.Palette.from(it).generate { palette ->
                        dominantColor = palette?.dominantSwatch?.rgb?.let { rgb -> Color(rgb) }
                        secondaryColor = palette?.vibrantSwatch?.rgb?.let { rgb -> Color(rgb) }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    val defaultBg = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val animatedDominant by androidx.compose.animation.animateColorAsState(dominantColor ?: defaultBg, label = "dominant")
    val animatedSecondary by androidx.compose.animation.animateColorAsState(secondaryColor ?: primaryColor, label = "secondary")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedDominant)
    ) {
        // Blurred background effect simulation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedSecondary.copy(alpha=0.6f),
                            animatedDominant
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha=0.3f))
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Close", modifier = Modifier.size(24.dp), tint = Color.White)
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = Color.White.copy(alpha=0.8f)
                )
                IconButton(
                    onClick = { /* More options */ },
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha=0.3f))
                ) {
                    Icon(Icons.Rounded.MoreVert, "Options", modifier = Modifier.size(24.dp), tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Hero Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.Black.copy(alpha=0.2f))
                    .border(1.dp, Color.White.copy(alpha=0.2f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = song.uri,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Rounded.GraphicEq)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha=0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.FavoriteBorder, "Like", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            val progress = if (song.duration > 0) state.currentPosition.toFloat() / song.duration.toFloat() else 0f
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { viewModel.seekTo((it * song.duration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha=0.3f)
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha=0.7f))
                Text(formatTime(song.duration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha=0.7f))
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                AnimatedIconButton(
                    onClick = { viewModel.togglePlayback() },
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = animatedDominant,
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White)
                )
                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { /* Repeat */ }) {
                    Icon(Icons.Rounded.Repeat, "Repeat", tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun AnimatedIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, selected: Boolean = false) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.2f else 1f)
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.scale(scale)
    )
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: Color = LocalContentColor.current
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.8f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    
    IconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.fillMaxSize(0.8f))
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun AdsPlayNowScreen(onClose: () -> Unit) {
    var timeLeft by remember { mutableStateOf(15) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (timeLeft == 0) onClose() },
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha=0.5f))
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Close", modifier = Modifier.size(24.dp), tint = if (timeLeft == 0) Color.White else Color.Gray)
                }
                Text(
                    text = "ADVERTISEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = if (timeLeft > 0) "$timeLeft s" else "Done",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Hero Ad Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Diamond, contentDescription = "Premium", modifier = Modifier.size(80.dp), tint = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("BEAT FLOW PREMIUM", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
                    Text("Ad-free music, offline listening.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha=0.8f))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Upgrade to Premium",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = "Enjoy endless music without interruptions.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.LightGray,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("GET PREMIUM NOW", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}


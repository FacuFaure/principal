package com.example.ipodnano

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photos
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ipodnano.player.PlayerViewModel
import com.example.ipodnano.player.Track

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                IpodNanoApp()
            }
        }
    }
}

private enum class NanoScreen(val label: String) {
    Home("Menu"),
    Music("Music"),
    NowPlaying("Now Playing"),
    Settings("Settings"),
    Photos("Photos"),
    Radio("Radio")
}

@Composable
private fun IpodNanoApp(playerViewModel: PlayerViewModel = viewModel()) {
    var screen by remember { mutableStateOf(NanoScreen.Home) }
    val accent = Color(0xFF41C7F3)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            playerViewModel.loadTracks(contentResolver = playerViewModel.getApplication<Application>().contentResolver)
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            playerViewModel.getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            playerViewModel.loadTracks(
                contentResolver = playerViewModel.getApplication<Application>().contentResolver
            )
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111318), Color(0xFF050607))
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = screen.label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (screen) {
                NanoScreen.Home -> HomeScreen(onSelect = { screen = it }, accent = accent)
                NanoScreen.Music -> MusicScreen(
                    playerViewModel = playerViewModel,
                    onBack = { screen = NanoScreen.Home },
                    accent = accent
                )
                NanoScreen.NowPlaying -> NowPlayingScreen(
                    playerViewModel = playerViewModel,
                    onBack = { screen = NanoScreen.Home },
                    accent = accent
                )
                NanoScreen.Settings -> PlaceholderScreen(
                    title = "Settings",
                    onBack = { screen = NanoScreen.Home }
                )
                NanoScreen.Photos -> PlaceholderScreen(
                    title = "Photos",
                    onBack = { screen = NanoScreen.Home }
                )
                NanoScreen.Radio -> PlaceholderScreen(
                    title = "Radio",
                    onBack = { screen = NanoScreen.Home }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FooterHint(text = "Swipe o toca para navegar")
        }
    }
}

@Composable
private fun HomeScreen(onSelect: (NanoScreen) -> Unit, accent: Color) {
    val items = listOf(
        NanoMenuItem("Music", Icons.Default.MusicNote, NanoScreen.Music),
        NanoMenuItem("Now Playing", Icons.Default.Headphones, NanoScreen.NowPlaying),
        NanoMenuItem("Radio", Icons.Default.Radio, NanoScreen.Radio),
        NanoMenuItem("Photos", Icons.Default.Photos, NanoScreen.Photos),
        NanoMenuItem("Albums", Icons.Default.Album, NanoScreen.Music),
        NanoMenuItem("Settings", Icons.Default.Settings, NanoScreen.Settings)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            Button(
                onClick = { onSelect(item.destination) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = CircleShape,
                modifier = Modifier
                    .size(140.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class NanoMenuItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destination: NanoScreen
)

@Composable
private fun MusicScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    accent: Color
) {
    val tracks by playerViewModel.tracks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "Library", onBack = onBack, accent = accent)
        Divider(color = Color(0xFF2E333F))
        if (tracks.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(tracks) { track ->
                    TrackRow(track = track, onPlay = { playerViewModel.playTrack(track) })
                }
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    accent: Color
) {
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val positionMs by playerViewModel.positionMs.collectAsState()
    val durationMs by playerViewModel.durationMs.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = "Now Playing", onBack = onBack, accent = accent)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color(0xFF1C1F24), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentTrack?.title ?: "Selecciona una canción",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = currentTrack?.artist ?: "",
            color = Color(0xFFB4B8C2)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { playerViewModel.seekTo(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs),
                color = Color(0xFFB4B8C2),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(durationMs),
                color = Color(0xFFB4B8C2),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundControlButton(
                onClick = { playerViewModel.skipPrevious() },
                icon = Icons.Default.FastRewind,
                accent = accent,
                contentDescription = "Anterior"
            )
            RoundControlButton(
                onClick = { playerViewModel.togglePlayback() },
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                accent = accent,
                contentDescription = if (isPlaying) "Pausa" else "Reproducir"
            )
            RoundControlButton(
                onClick = { playerViewModel.skipNext() },
                icon = Icons.Default.FastForward,
                accent = accent,
                contentDescription = "Siguiente"
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(title = title, onBack = onBack, accent = Color(0xFF41C7F3))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "En construcción",
            color = Color(0xFFB4B8C2)
        )
    }
}

@Composable
private fun SectionHeader(title: String, onBack: () -> Unit, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1F24)),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Text(text = "<", color = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Box(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun TrackRow(track: Track, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = track.title, color = Color.White)
                Text(text = track.artist, color = Color(0xFFB4B8C2))
            }
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3039)),
                shape = CircleShape
            ) {
                Text(text = "Play", color = Color.White)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No se encontraron canciones locales.",
            color = Color(0xFFB4B8C2),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agrega música al dispositivo y vuelve a intentar.",
            color = Color(0xFFB4B8C2),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FooterHint(text: String) {
    Text(
        text = text,
        color = Color(0xFF7C808A),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RoundControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    contentDescription: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

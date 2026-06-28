package com.tranik.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.tranik.app.data.model.Track
import com.tranik.app.ui.screens.*
import com.tranik.app.ui.theme.*
import com.tranik.app.ui.viewmodel.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val hasPermission = remember { mutableStateOf(checkAudioPermission()) }
            if (hasPermission.value) {
                TarAnikAppContent()
            } else {
                PermissionScreen(
                    onGranted = { hasPermission.value = true },
                    onDeny = { hasPermission.value = false }
                )
            }
        }
    }

    private fun checkAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun PermissionScreen(onGranted: () -> Unit, onDeny: () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onGranted() else onDeny() }
    Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🎵 ترانیک", fontSize = 32.sp, color = Color.Transparent)
            Spacer(Modifier.height(24.dp))
            Text("برای دسترسی به آهنگ‌ها، اجازه خواندن فایل‌های صوتی لازم است", color = Text2, fontSize = 15.sp)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { launcher.launch(permission) }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("اجازه دسترسی", fontSize = 16.sp)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Library : Screen("library", "کتابخانه")
    object Player : Screen("player", "پلیر")
    object Tags : Screen("tags", "ادیت")
    object Lyrics : Screen("lyrics", "لایریک")
    object Sync : Screen("sync", "همگام")
    object Translate : Screen("translate", "ترجمه")
    object Export : Screen("export", "خروجی")
    object Settings : Screen("settings", "تنظیمات")
}

@Composable
fun TarAnikAppContent() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val playerVm: PlayerViewModel = hiltViewModel()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val playerState by playerVm.state.collectAsState()
    val currentTrack = playerState.currentTrack
    val isPlaying = playerState.isPlaying
    val showMiniPlayer = currentTrack != null && currentRoute != Screen.Player.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        libraryVm.globalError.collect { error ->
            error?.let {
                snackbarHostState.showSnackbar(it)
           libraryVm.clearError()
            }
        }
    }

    LaunchedEffect(playerState.queueIndex) {
        val queued = playerVm.getQueuedTrack()
        if (queued != null && queued != currentTrack) {
            val context = navController.context
            playerVm.playTrack(queued, context, playerState.queue)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                if (showMiniPlayer) {
                    MiniPlayer(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        albumArtUri = if (currentTrack.albumId >= 0) playerVm.getAlbumArtUri(currentTrack.albumId) else null,
                        onPlayPause = { playerVm.togglePlay() },
                        onClick = { navController.navigate(Screen.Player.route) }
                    )
                }
                if (currentRoute in listOf(Screen.Library.route, Screen.Player.route, Screen.Tags.route, Screen.Lyrics.route, Screen.Settings.route)) {
                    BottomNav(currentRoute = currentRoute, onNav = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                enterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(300)) }
            ) {
                composable(Screen.Library.route) {
                    LibraryScreen(vm = libraryVm, playerVm = playerVm, onPlay = { track, context ->
                        val allTracks = libraryVm.state.value.filteredTracks
                        playerVm.playTrack(track, context, allTracks)
                    }, onEdit = { navController.navigate(Screen.Tags.route) })
                }
                composable(Screen.Player.route) { PlayerScreen(vm = playerVm) }
                composable(Screen.Tags.route) {
                    val tagVm: TagEditorViewModel = hiltViewModel()
                    LaunchedEffect(currentTrack) { currentTrack?.let { tagVm.loadTrack(it) } }
                    TagEditorScreen(vm = tagVm, onBack = { navController.popBackStack() })
                }
                composable(Screen.Lyrics.route) {
                    val lyricsVm: LyricsViewModel = hiltViewModel()
                    LaunchedEffect(currentTrack) { currentTrack?.let { lyricsVm.loadTrack(it) } }
                    LyricsEditorScreen(vm = lyricsVm, onNavigateToSync = { navController.navigate(Screen.Sync.route) }, onBack = { navController.popBackStack() })
                }
                composable(Screen.Sync.route) {
                    val syncVm: SyncViewModel = hiltViewModel()
                    val lyricsVm: LyricsViewModel = hiltViewModel()
                    LaunchedEffect(currentTrack) { currentTrack?.let { lyricsVm.loadTrack(it) } }
                    SyncScreen(vm = syncVm, playerVm = playerVm, lyricsVm = lyricsVm, onBack = { navController.popBackStack() })
                }
                composable(Screen.Translate.route) {
                    val translateVm: TranslateViewModel = hiltViewModel()
                    TranslateScreen(vm = translateVm, currentTrack = currentTrack, onBack = { navController.popBackStack() })
                }
                composable(Screen.Export.route) {
                    val exportVm: ExportViewModel = hiltViewModel()
                    ExportScreen(vm = exportVm, currentTrack = currentTrack, onBack = { navController.popBackStack() })
                }
                composable(Screen.Settings.route) {
                    val settingsVm: SettingsViewModel = hiltViewModel()
                    SettingsScreen(vm = settingsVm, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun BottomNav(currentRoute: String?, onNav: (Screen) -> Unit) {
    val items = listOf(Screen.Library, Screen.Player, Screen.Tags, Screen.Lyrics, Screen.Settings)
    NavigationBar(containerColor = Color(0xFF13131A)) {
        items.forEach { screen ->
            val selected = when (screen) {
                Screen.Settings -> currentRoute in listOf(Screen.Settings.route, Screen.Export.route, Screen.Translate.route, Screen.Sync.route)
                else -> currentRoute == screen.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNav(screen) },
                icon = { Icon(when (screen) {
                    Screen.Library -> Icons.Default.List
                    Screen.Player -> Icons.Default.PlayArrow
                    Screen.Tags -> Icons.Default.Edit
                    Screen.Lyrics -> Icons.Default.TextFields
                    Screen.Settings -> Icons.Default.Settings
                    else -> Icons.Default.MoreHoriz
                }, contentDescription = screen.label) },
                label = { Text(screen.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF7C6FE0),
                    selectedTextColor = Color(0xFF7C6FE0),
                    unselectedIconColor = Color(0xFF8888A0),
                    indicatorColor = Color(0xFF21212E)
                )
            )
        }
    }
}

@Composable
fun MiniPlayer(track: Track, isPlaying: Boolean, albumArtUri: android.net.Uri?, onPlayPause: () -> Unit, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color(0xFF1A1A24), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (albumArtUri != null) {
                    coil.compose.AsyncImage(model = albumArtUri, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(DarkBg3), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.displayName, fontSize = 13.sp, color = Text1, maxLines = 1)
                Text(track.displayArtist, fontSize = 11.sp, color = Text3, maxLines = 1)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Accent)
            }
        }
    }
}

/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.openaudio.data.repository.OpenAudioRepository
import org.openaudio.feature.dac.DeviceInformationScreen
import org.openaudio.feature.dac.UsbDacManagerScreen
import org.openaudio.feature.equalizer.EqualizerScreen
import org.openaudio.feature.home.HomeScreen
import org.openaudio.feature.library.FolderBrowserScreen
import org.openaudio.feature.library.MusicLibraryScreen
import org.openaudio.feature.player.NowPlayingScreen
import org.openaudio.feature.settings.AboutOpenAudioScreen
import org.openaudio.feature.settings.AudioEngineScreen
import org.openaudio.feature.settings.AudioSettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAudioAppScaffold(
    repository: OpenAudioRepository
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: OpenAudioDestination.HOME.route

    val settings by repository.preferences.settingsFlow.collectAsState()
    val connectedDac by repository.usbDetector.connectedDac.collectAsState()
    val telemetry by repository.audioCoordinator.telemetry.collectAsState()
    val scannedTracks by repository.scannedTracks.collectAsState()
    val isScanning by repository.isScanning.collectAsState()
    val scanStatusMessage by repository.scanStatusMessage.collectAsState()
    val currentTrack by repository.currentTrack.collectAsState()
    val isPlaying by repository.isPlaying.collectAsState()
    val currentPositionMs by repository.currentPositionMs.collectAsState()
    val durationMs by repository.durationMs.collectAsState()
    val shuffleEnabled by repository.shuffleEnabled.collectAsState()
    val repeatAllEnabled by repository.repeatAllEnabled.collectAsState()

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OpenAudio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (connectedDac != null) Color(0xFF003B46) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { navigateTo(OpenAudioDestination.USB_DAC_MANAGER.route) }
                        ) {
                            Text(
                                text = if (connectedDac != null) "USB DAC AKTIF" else "SPEAKER HP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectedDac != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { navigateTo(OpenAudioDestination.EQUALIZER.route) }) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Equalizer")
                    }
                    IconButton(onClick = { navigateTo(OpenAudioDestination.DEVICE_INFO.route) }) {
                        Icon(Icons.Default.Memory, contentDescription = "Info Perangkat")
                    }
                    IconButton(onClick = { navigateTo(OpenAudioDestination.AUDIO_SETTINGS.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Floating Mini-Player Bar (visible whenever a track is loaded and not already on Now Playing screen)
                if (currentTrack != null && currentRoute != OpenAudioDestination.NOW_PLAYING.route) {
                    Surface(
                        color = Color(0xFF161C28),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigateTo(OpenAudioDestination.NOW_PLAYING.route) }
                    ) {
                        Column {
                            val progress = (currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack?.title ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentTrack?.artist} • ${currentTrack?.format?.label} ${currentTrack?.bitDepth}-bit/${(currentTrack?.sampleRateHz ?: 44100) / 1000.0}kHz",
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { repository.togglePlayPause() }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Putar/Jeda"
                                    )
                                }
                                IconButton(onClick = { repository.skipToNext(context) }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Berikutnya"
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation Bar with Clear Icons
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    NavigationBarItem(
                        selected = currentRoute == OpenAudioDestination.HOME.route,
                        onClick = { navigateTo(OpenAudioDestination.HOME.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == OpenAudioDestination.MUSIC_LIBRARY.route,
                        onClick = { navigateTo(OpenAudioDestination.MUSIC_LIBRARY.route) },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Lagu") },
                        label = { Text("Lagu") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == OpenAudioDestination.FOLDER_BROWSER.route,
                        onClick = { navigateTo(OpenAudioDestination.FOLDER_BROWSER.route) },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Folder") },
                        label = { Text("Folder") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == OpenAudioDestination.NOW_PLAYING.route,
                        onClick = { navigateTo(OpenAudioDestination.NOW_PLAYING.route) },
                        icon = { Icon(Icons.Default.Album, contentDescription = "Pemutar") },
                        label = { Text("Pemutar") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == OpenAudioDestination.USB_DAC_MANAGER.route,
                        onClick = { navigateTo(OpenAudioDestination.USB_DAC_MANAGER.route) },
                        icon = { Icon(Icons.Default.Usb, contentDescription = "USB DAC") },
                        label = { Text("USB DAC") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OpenAudioDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(OpenAudioDestination.HOME.route) {
                HomeScreen(
                    connectedDac = connectedDac,
                    telemetry = telemetry,
                    totalTracksCount = scannedTracks.size,
                    onQuickScanMusic = { repository.scanDeviceMediaStore(context) },
                    onQuickScanDac = { repository.scanAndConnectUsbDac(context) },
                    onNavigateToRoute = navigateTo
                )
            }
            composable(OpenAudioDestination.MUSIC_LIBRARY.route) {
                MusicLibraryScreen(
                    tracks = scannedTracks,
                    isScanning = isScanning,
                    scanStatusMessage = scanStatusMessage,
                    onRequestScanMediaStore = { repository.scanDeviceMediaStore(context) },
                    onPickSafFolder = { uri -> repository.scanSafFolder(context, uri) },
                    onSelectTrack = { track ->
                        repository.selectAndPlayTrack(context, track)
                        navigateTo(OpenAudioDestination.NOW_PLAYING.route)
                    }
                )
            }
            composable(OpenAudioDestination.FOLDER_BROWSER.route) {
                FolderBrowserScreen(
                    scanStatusMessage = scanStatusMessage,
                    onPickSafFolder = { uri ->
                        repository.scanSafFolder(context, uri)
                        navigateTo(OpenAudioDestination.MUSIC_LIBRARY.route)
                    }
                )
            }
            composable(OpenAudioDestination.NOW_PLAYING.route) {
                NowPlayingScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    shuffleEnabled = shuffleEnabled,
                    repeatAllEnabled = repeatAllEnabled,
                    onTogglePlayPause = { repository.togglePlayPause() },
                    onSeekTo = { pos -> repository.seekToPosition(pos) },
                    onSkipNext = { repository.skipToNext(context) },
                    onSkipPrevious = { repository.skipToPrevious(context) },
                    onToggleShuffle = { repository.toggleShuffle() },
                    onToggleRepeat = { repository.toggleRepeat() },
                    telemetry = telemetry,
                    connectedDac = connectedDac
                )
            }
            composable(OpenAudioDestination.USB_DAC_MANAGER.route) {
                UsbDacManagerScreen(
                    connectedDac = connectedDac,
                    telemetry = telemetry,
                    onScanUsbDac = { repository.scanAndConnectUsbDac(context) }
                )
            }
            composable(OpenAudioDestination.AUDIO_ENGINE.route) {
                AudioEngineScreen(
                    settings = settings,
                    telemetry = telemetry,
                    onSelectPath = { repository.preferences.setPreferredOutputPath(it) }
                )
            }
            composable(OpenAudioDestination.EQUALIZER.route) {
                EqualizerScreen(
                    settings = settings,
                    onToggleDspEqualizer = { repository.preferences.setDspEqualizerEnabled(it) }
                )
            }
            composable(OpenAudioDestination.AUDIO_SETTINGS.route) {
                AudioSettingsScreen(
                    settings = settings,
                    onToggleBitPerfectLock = { repository.preferences.setBitPerfectModeLocked(it) }
                )
            }
            composable(OpenAudioDestination.DEVICE_INFO.route) {
                DeviceInformationScreen(
                    connectedDac = connectedDac,
                    telemetry = telemetry,
                    feasibilityEntries = repository.feasibilityMatrix
                )
            }
            composable(OpenAudioDestination.ABOUT.route) {
                AboutOpenAudioScreen()
            }
        }
    }
}

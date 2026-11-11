/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: OpenAudioDestination.HOME.route

    val settings by repository.preferences.settingsFlow.collectAsState()
    val connectedDac by repository.usbDetector.connectedDac.collectAsState()
    val telemetry by repository.audioCoordinator.telemetry.collectAsState()

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
                title = { Text("OpenAudio Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    TextButton(onClick = { navigateTo(OpenAudioDestination.DEVICE_INFO.route) }) {
                        Text("Diag")
                    }
                    TextButton(onClick = { navigateTo(OpenAudioDestination.AUDIO_SETTINGS.route) }) {
                        Text("Cfg")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                OpenAudioDestination.entries.filter { it.showInBottomBar }.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { navigateTo(dest.route) },
                        icon = { Text(dest.title.take(2).uppercase()) },
                        label = { Text(dest.title) }
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
                    onNavigateToRoute = navigateTo
                )
            }
            composable(OpenAudioDestination.MUSIC_LIBRARY.route) {
                MusicLibraryScreen(tracks = emptyList())
            }
            composable(OpenAudioDestination.FOLDER_BROWSER.route) {
                FolderBrowserScreen()
            }
            composable(OpenAudioDestination.NOW_PLAYING.route) {
                NowPlayingScreen(
                    telemetry = telemetry,
                    connectedDac = connectedDac
                )
            }
            composable(OpenAudioDestination.USB_DAC_MANAGER.route) {
                UsbDacManagerScreen(
                    connectedDac = connectedDac,
                    telemetry = telemetry
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

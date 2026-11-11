/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.player.navigation

/**
 * All 10 required screens from Specification Section H.
 */
enum class OpenAudioDestination(
    val route: String,
    val title: String,
    val showInBottomBar: Boolean
) {
    HOME("home", "Home", true),
    MUSIC_LIBRARY("library", "Library", true),
    FOLDER_BROWSER("folders", "Folders", false),
    NOW_PLAYING("now_playing", "Now Playing", true),
    USB_DAC_MANAGER("dac_manager", "USB DAC", true),
    AUDIO_ENGINE("audio_engine", "Engine", true),
    EQUALIZER("equalizer", "Equalizer", false),
    AUDIO_SETTINGS("audio_settings", "Settings", false),
    DEVICE_INFO("device_info", "Device Info", false),
    ABOUT("about", "About", false)
}

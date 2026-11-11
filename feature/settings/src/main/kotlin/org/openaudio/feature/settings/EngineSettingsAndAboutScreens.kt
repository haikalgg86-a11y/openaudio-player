/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openaudio.core.model.AppSettingsState
import org.openaudio.core.model.AudioOutputPath
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.BitPerfectVerificationLevel

@Composable
fun AudioEngineScreen(
    settings: AppSettingsState,
    telemetry: AudioTelemetrySnapshot,
    onSelectPath: (AudioOutputPath) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Dual Audio Output Engine & Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(AudioOutputPath.entries) { path ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.preferredOutputPath == path,
                        onClick = { onSelectPath(path) }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = path.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Text(
                text = "5-Level Bit-Perfect Verification Hierarchy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(BitPerfectVerificationLevel.entries) { level ->
            val isCurrent = telemetry.verificationLevel == level
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${level.title} ${if (isCurrent) "[CURRENT]" else ""}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = level.caveatDescription,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun AudioSettingsScreen(
    settings: AppSettingsState,
    onToggleBitPerfectLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Audio & USB Buffer Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bit-Perfect Mode Lock", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Bypasses all software EQ, DSP, crossfade, and digital volume scaling",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.bitPerfectModeLocked,
                onCheckedChange = onToggleBitPerfectLock
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ISOCHRONOUS URB PIPELINE PARAMETERS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Active URB Ring Depth: ${settings.urbRingCount} URBs", fontFamily = FontFamily.Monospace)
                Text("Microframes per URB (High-Speed): ${settings.microframesPerUrb} (1 ms/URB)", fontFamily = FontFamily.Monospace)
                Text("RAM Page Locking (mlock): ${if (settings.enableRamMlock) "Enabled" else "Disabled"}", fontFamily = FontFamily.Monospace)
                Text("UAC Hardware Volume (FU_VOLUME_CONTROL): ${if (settings.enableHardwareVolume) "Enabled (0.0 dBFS Unity)" else "Disabled"}", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun AboutOpenAudioScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "About OpenAudio Player",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version: 0.1.0-alpha01 (Phase 2 Foundation)", fontFamily = FontFamily.Monospace)
                    Text("License: Apache License, Version 2.0", fontFamily = FontFamily.Monospace)
                    Text("Target SDK: API 36 (Android 16 Custom ROM Priority) | Min SDK: API 26", fontFamily = FontFamily.Monospace)
                    Text(
                        text = "Audited Open-Source Dependencies: Android Jetpack (Apache-2.0), Xiph.Org libFLAC/libogg/libopus/libvorbis (BSD-3-Clause), dr_wav/dr_mp3 (MIT-0/Public Domain), Linux Kernel UAPI Headers (GPL-2.0 WITH Linux-syscall-note). Zero GPL/AGPL/LGPL copyleft dependencies in binary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

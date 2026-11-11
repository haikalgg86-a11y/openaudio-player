/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.DacDeviceInfo

@Composable
fun HomeScreen(
    connectedDac: DacDeviceInfo?,
    telemetry: AudioTelemetrySnapshot,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "OpenAudio Player",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hi-Res USB Audio Class 1.0/2.0 Engine • Android 16 (API 36)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE SIGNAL PATH & VERIFICATION STATUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = telemetry.activePath.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verification: ${telemetry.verificationLevel.title}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = telemetry.verificationLevel.caveatDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "USB DAC: ${connectedDac?.safeProduct ?: "Disconnected — Attach USB-C DAC via OTG"}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                text = "All 10 Application Screens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val quickRoutes = listOf(
                "library" to "2. Music Library",
                "folders" to "3. Folder Browser",
                "now_playing" to "4. Now Playing",
                "dac_manager" to "5. USB DAC Manager",
                "audio_engine" to "6. Audio Engine",
                "equalizer" to "7. Equalizer & DSP",
                "audio_settings" to "8. Audio Settings",
                "device_info" to "9. Device Information",
                "about" to "10. About OpenAudio"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickRoutes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { (route, label) ->
                            OutlinedButton(
                                onClick = { onNavigateToRoute(route) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = label, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

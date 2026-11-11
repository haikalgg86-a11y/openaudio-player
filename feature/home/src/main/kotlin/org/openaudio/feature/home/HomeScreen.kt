/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openaudio.core.model.AudioOutputPath
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.DacDeviceInfo

@Composable
fun HomeScreen(
    connectedDac: DacDeviceInfo?,
    telemetry: AudioTelemetrySnapshot,
    totalTracksCount: Int,
    onQuickScanMusic: () -> Unit,
    onQuickScanDac: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Visual Signal Chain Banner (Easy to understand for beginners & audiophiles)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (connectedDac != null) Icons.Default.Usb else Icons.Default.Headphones,
                                contentDescription = null,
                                tint = if (connectedDac != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (connectedDac != null) "USB DAC TERHUBUNG" else "MODE SPEAKER / AUDIO HP",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedDac != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (telemetry.dspBypassed) Color(0xFF003D20) else Color(0xFF3E2723)
                        ) {
                            Text(
                                text = if (telemetry.dspBypassed) "BIT-PERFECT SIAP" else "EQ / DSP AKTIF",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.dspBypassed) Color(0xFF00E676) else Color(0xFFFFB300)
                            )
                        }
                    }

                    // 3-Step Visual Chain: File -> Engine -> Output
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SUMBER FILE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${telemetry.sourceFormat.label} ${telemetry.sourceBitDepth}-bit",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${telemetry.sourceSampleRateHz / 1000.0} kHz",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Text("➔", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MESIN AUDIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (telemetry.activePath == AudioOutputPath.PATH_B_DIRECT_USB) "Direct USB" else "Android Native",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = telemetry.verificationLevel.code,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Text("➔", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PERANGKAT DAC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = connectedDac?.safeProduct?.take(12) ?: "Audio Internal",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${telemetry.outputSampleRateHz / 1000.0} kHz",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Text(
                        text = if (connectedDac == null) {
                            "Tips: Anda bisa langsung memutar musik lewat speaker/headset HP sekarang, atau mencolokkan dongle USB-C DAC untuk kualitas Hi-Res Bit-Perfect."
                        } else {
                            "DAC Aktif: ${connectedDac.safeProduct} (${connectedDac.uacVersion.label})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onQuickScanMusic()
                                onNavigateToRoute("library")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (totalTracksCount > 0) "Buka Lagu ($totalTracksCount)" else "Cari Lagu di HP")
                        }
                        OutlinedButton(
                            onClick = {
                                onQuickScanDac()
                                onNavigateToRoute("dac_manager")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cek USB DAC")
                        }
                    }
                }
            }
        }

        // 2. Easy Menu Grid for All 10 Screens
        item {
            Text(
                text = "Menu Utama & Pengaturan Audio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val menuCards = listOf(
                MenuCardItem("library", "Pustaka Musik", "Daftar semua lagu & pencarian", Icons.Default.LibraryMusic),
                MenuCardItem("folders", "Jelajah Folder", "Pilih folder FLAC / DSD manual", Icons.Default.FolderOpen),
                MenuCardItem("now_playing", "Sedang Diputar", "Pemutar penuh & info bit-rate", Icons.Default.Album),
                MenuCardItem("dac_manager", "USB DAC Manager", "Hubungkan & cek dongle DAC", Icons.Default.Usb),
                MenuCardItem("audio_engine", "Mode Bit-Perfect", "Pilih Direct USB vs Android Audio", Icons.Default.CheckCircle),
                MenuCardItem("equalizer", "Equalizer & Efek", "10-Band Parametric EQ", Icons.Default.Equalizer),
                MenuCardItem("audio_settings", "Pengaturan Buffer", "Atur ukuran buffer USB & volume", Icons.Default.Settings),
                MenuCardItem("device_info", "Info Perangkat", "Diagnosa sistem & kernel Android 16", Icons.Default.Memory),
                MenuCardItem("about", "Tentang Aplikasi", "Lisensi open-source Apache 2.0", Icons.Default.Info)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                menuCards.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToRoute(item.route) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private data class MenuCardItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

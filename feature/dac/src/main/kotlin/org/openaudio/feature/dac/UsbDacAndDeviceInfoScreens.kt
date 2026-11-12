/*
 * Copyright 2026 OpenAudio Player Contributors
 * Licensed under the Apache License, Version 2.0
 */
package org.openaudio.feature.dac

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openaudio.core.model.AudioTelemetrySnapshot
import org.openaudio.core.model.DacDeviceInfo
import org.openaudio.usb.transfer.UsbFeasibilityAuditEntry

@Composable
fun UsbDacManagerScreen(
    connectedDac: DacDeviceInfo?,
    telemetry: AudioTelemetrySnapshot,
    onScanUsbDac: () -> Unit,
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
                text = "USB DAC Manager",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Button(
                onClick = onScanUsbDac,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan & Request USB DAC Permission")
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (connectedDac == null) {
                        Text(
                            text = "Connection Status: Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Connect a USB Audio Class 1.0 or 2.0 dongle/DAC via USB-C OTG, then tap 'Scan & Request USB DAC Permission' above.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Connection Status: Connected (Permission: ${if (connectedDac.permissionGranted) "Granted" else "Pending — Tap Scan Again After Allowing"})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Manufacturer: ${connectedDac.safeManufacturer}", fontFamily = FontFamily.Monospace)
                        Text("Model: ${connectedDac.safeProduct}", fontFamily = FontFamily.Monospace)
                        Text("USB VID:PID: ${connectedDac.formattedVidPid}", fontFamily = FontFamily.Monospace)
                        Text("UAC Version: ${connectedDac.uacVersion.label}", fontFamily = FontFamily.Monospace)
                        Text("Bound Kernel Driver: ${connectedDac.kernelDriverBound ?: "None"}", fontFamily = FontFamily.Monospace)
                        Text("Supported Bit Depths: ${connectedDac.supportedBitDepths.joinToString(", ")}-bit", fontFamily = FontFamily.Monospace)
                        Text("Supported Sample Rates: ${connectedDac.supportedSampleRatesHz.joinToString(", ")} Hz", fontFamily = FontFamily.Monospace)
                        Text("Active Output Mode: ${telemetry.activePath.displayName}", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInformationScreen(
    connectedDac: DacDeviceInfo?,
    telemetry: AudioTelemetrySnapshot,
    feasibilityEntries: List<UsbFeasibilityAuditEntry>,
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
                text = "Device & USBFS Feasibility Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("HOST ANDROID ENVIRONMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("Android SDK Level: API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("Primary ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("Runtime Kernel Page Size: ${telemetry.systemPageSizeBytes} bytes (16 KB ELF Aligned)", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text("USB Descriptor Node: ${connectedDac?.deviceNamePath ?: "None"}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text(
                text = "Phase 1.1 Non-Root USBFS Feasibility Matrix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(feasibilityEntries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = entry.capabilityName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Status: ${entry.category.label}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Mechanism: ${entry.mechanism}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = entry.runtimeNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncDeviceInfo
import com.example.data.sync.SyncState
import com.example.data.sync.SyncStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MultiDeviceSyncScreen(
    syncStatus: SyncStatus,
    onJoinSyncGroup: (String) -> Unit,
    onSimulateRemoteSync: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var isSimulating by remember { mutableStateOf(false) }
    var simulationResultToast by remember { mutableStateOf<String?>(null) }

    val lastSyncFormatted = remember(syncStatus.lastSyncedTime) {
        if (syncStatus.lastSyncedTime > 0) {
            SimpleDateFormat("h:mm:ss a, MMM d", Locale.getDefault()).format(Date(syncStatus.lastSyncedTime))
        } else "Never"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Text(
                text = "Real-Time Multi-Device Sync",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Instant live synchronization across phones, tablets & family",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Sync Room Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_room_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Sync Group ID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = syncStatus.syncGroupId,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Sync Group ID", syncStatus.syncGroupId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${syncStatus.syncGroupId} to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_sync_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Sync ID", tint = EmeraldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sync State Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (syncStatus.state) {
                            SyncState.CONNECTED_LIVE -> EmeraldPrimary.copy(alpha = 0.15f)
                            SyncState.CONNECTING -> TealSecondary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (syncStatus.state) {
                                            SyncState.CONNECTED_LIVE -> EmeraldPrimary
                                            SyncState.CONNECTING -> TealSecondary
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncStatus.message,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (syncStatus.state) {
                                    SyncState.CONNECTED_LIVE -> EmeraldPrimary
                                    SyncState.CONNECTING -> TealSecondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Last synced: $lastSyncFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Test Cross-Device Sync Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Test Multi-Device Sync in Emulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulate another device (e.g. Partner's phone or tablet) logging an expense to test instant real-time synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isSimulating = true
                            onSimulateRemoteSync { msg ->
                                isSimulating = false
                                simulationResultToast = msg
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSimulating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("simulate_remote_sync_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulating Remote Device...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Remote Device Sync", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (simulationResultToast != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ $simulationResultToast",
                            color = EmeraldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Join Existing Sync Room
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Pair with Another Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a Sync Group ID from another phone or family member to merge data into one live workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = joinRoomCodeInput,
                            onValueChange = { joinRoomCodeInput = it.uppercase() },
                            placeholder = { Text("e.g. SYNC-8924") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_sync_group_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (joinRoomCodeInput.isNotBlank()) {
                                    onJoinSyncGroup(joinRoomCodeInput)
                                    Toast.makeText(context, "Joined workspace $joinRoomCodeInput", Toast.LENGTH_SHORT).show()
                                    joinRoomCodeInput = ""
                                }
                            },
                            enabled = joinRoomCodeInput.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("join_sync_group_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Join")
                        }
                    }
                }
            }
        }

        // Connected Devices List
        item {
            Text(
                text = "Paired Devices in this Workspace (${syncStatus.connectedDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(syncStatus.connectedDevices) { device ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (device.isCurrentDevice) EmeraldPrimary.copy(alpha = 0.15f) else TealSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (device.isCurrentDevice) Icons.Default.Smartphone else Icons.Default.TabletAndroid,
                            contentDescription = null,
                            tint = if (device.isCurrentDevice) EmeraldPrimary else TealSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Device ID: ${device.deviceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Live Active",
                            color = EmeraldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

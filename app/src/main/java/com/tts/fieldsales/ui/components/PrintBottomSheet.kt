package com.tts.fieldsales.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.print.BluetoothPrinterManager
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet shown when user taps a Print button.
 * Three options:
 *   1. Preview  — navigate to full-screen WebView preview
 *   2. Bluetooth Print — send to paired BT thermal printer
 *   3. System Print — Android print dialog (PDF, AirPrint, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBottomSheet(
    reportName: String,
    recordId: Int,
    recordName: String = "Document",
    onDismiss: () -> Unit,
    onPreview: (() -> Unit)? = null   // called when user wants preview (navigate to PrintPreviewScreen)
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showBtList by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var paperWidthMm by remember { mutableStateOf(58) }

    // Load paper width from prefs on start
    LaunchedEffect(Unit) { paperWidthMm = prefs.getPaperWidth() }

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) showBtList = true
        else status = "Bluetooth permission required."
    }

    fun requestBtPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_CONNECT
            perms += Manifest.permission.BLUETOOTH_SCAN
        } else {
            perms += Manifest.permission.BLUETOOTH
            perms += Manifest.permission.BLUETOOTH_ADMIN
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) showBtList = true
        else permLauncher.launch(needed.toTypedArray())
    }

    fun loadPairedDevices() {
        try {
            val btManager = context.getSystemService(BluetoothManager::class.java)
            val btAdapter = btManager?.adapter
            if (btAdapter == null || !btAdapter.isEnabled) {
                status = "Bluetooth is off. Please enable it in Settings."
                return
            }
            pairedDevices = btAdapter.bondedDevices
                .filter { it.name != null }
                .sortedBy { it.name }
        } catch (e: SecurityException) {
            status = "Bluetooth permission denied."
        } catch (e: Exception) {
            status = "Error: ${e.message}"
        }
    }

    // Auto-load devices when sheet shown
    LaunchedEffect(showBtList) {
        if (showBtList) loadPairedDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BrownDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GoldDim) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Print / Preview", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(recordName, style = MaterialTheme.typography.bodySmall, color = GoldDim)
                }
                // Paper size chip
                FilterChip(
                    selected = true,
                    onClick = {
                        scope.launch {
                            val newWidth = if (paperWidthMm == 58) 80 else 58
                            paperWidthMm = newWidth
                            prefs.setPaperWidth(newWidth)
                        }
                    },
                    label = { Text(if (paperWidthMm == 80) "4\" 80mm" else "3\" 58mm", color = TextOnGold, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary),
                    leadingIcon = { Icon(Icons.Default.Straighten, null, tint = TextOnGold, modifier = Modifier.size(14.dp)) }
                )
            }

            GoldDivider(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            // ── Option 1: Preview ─────────────────────────────────────────────
            PrintOption(
                icon = Icons.Default.Visibility,
                title = "Preview Template",
                subtitle = "View the print template in the app",
                iconTint = StatusBlue
            ) {
                onDismiss()
                onPreview?.invoke()
            }

            // ── Option 2: Bluetooth Print ─────────────────────────────────────
            PrintOption(
                icon = Icons.Default.BluetoothSearching,
                title = "Bluetooth Thermal Printer",
                subtitle = "Send directly to paired thermal printer",
                iconTint = Color(0xFF29B6F6)
            ) {
                requestBtPermissions()
            }

            // ── Option 3: System Print ────────────────────────────────────────
            PrintOption(
                icon = Icons.Default.Print,
                title = "System Print / PDF",
                subtitle = "Android print dialog (save as PDF or use cloud print)",
                iconTint = StatusGreen
            ) {
                scope.launch {
                    isLoading = true
                    status = "Fetching template..."
                    withContext(Dispatchers.IO) {
                        val repo = OdooRepository(prefs)
                        repo.getReportHtml(reportName, recordId).fold(
                            onSuccess = { html ->
                                status = "Opening print dialog..."
                            },
                            onFailure = { e ->
                                status = "Failed to load: ${e.message?.take(60)}"
                            }
                        )
                    }
                    isLoading = false
                }
            }

            // Status message
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .background(
                            if (status.startsWith("Failed") || status.contains("denied")) StatusRed.copy(0.1f) else StatusAmber.copy(0.1f),
                            RoundedCornerShape(10.dp)
                        ).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (status.startsWith("Failed")) Icons.Default.Error else Icons.Default.Info,
                        null,
                        tint = if (status.startsWith("Failed")) StatusRed else StatusAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(status, color = if (status.startsWith("Failed")) StatusRed else StatusAmber, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = GoldPrimary, trackColor = GoldDim.copy(0.2f))
            }

            // ── Bluetooth device list ─────────────────────────────────────────
            if (showBtList) {
                Spacer(Modifier.height(12.dp))
                GoldDivider(Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select Printer",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))
                if (pairedDevices.isEmpty()) {
                    Text(
                        "No paired Bluetooth devices found.\nPair your thermal printer in Android Settings first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pairedDevices, key = { it.address }) { device ->
                            BluetoothDeviceRow(
                                device = device,
                                isLoading = isLoading,
                                onSelect = {
                                    scope.launch {
                                        isLoading = true
                                        status = "Connecting to ${device.name}..."
                                        withContext(Dispatchers.IO) {
                                            val repo = OdooRepository(prefs)
                                            repo.getReportHtml(reportName, recordId).fold(
                                                onSuccess = { html ->
                                                    val btPrinter = BluetoothPrinterManager(context)
                                                    btPrinter.printHtml(device, html, prefs.getPaperWidth()).fold(
                                                        onSuccess = { status = "✓ Printed to ${device.name}!" },
                                                        onFailure = { e -> status = "Failed: ${e.message}" }
                                                    )
                                                },
                                                onFailure = { e ->
                                                    status = "Template error: ${e.message}"
                                                }
                                            )
                                        }
                                        isLoading = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PrintOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(46.dp)
                    .background(iconTint.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BluetoothDeviceRow(
    device: BluetoothDevice,
    isLoading: Boolean,
    onSelect: () -> Unit
) {
    val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
    val address = device.address

    Surface(
        onClick = onSelect,
        enabled = !isLoading,
        color = BrownMedium.copy(0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.BluetoothConnected, null, tint = Color(0xFF29B6F6), modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = TextPrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                Text(address, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoldPrimary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Print, null, tint = GoldDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

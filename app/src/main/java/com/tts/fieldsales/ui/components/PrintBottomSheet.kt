package com.tts.fieldsales.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.print.BluetoothPrinterManager
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintBottomSheet(
    reportName: String,
    recordId: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    val btManager = remember { BluetoothPrinterManager(context) }
    val scope = rememberCoroutineScope()

    var paperWidth by remember { mutableStateOf("3inch") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    LaunchedEffect(Unit) {
        paperWidth = prefs.getPaperWidth()
        pairedDevices = btManager.getPairedPrinters()
        val savedAddress = prefs.getPrinterAddress()
        selectedDevice = pairedDevices.firstOrNull { it.address == savedAddress }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BrownDark,
        dragHandle = {
            Box(Modifier.padding(8.dp)) {
                Box(Modifier.width(40.dp).height(4.dp).background(GoldDim.copy(0.5f), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Print, null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                Text("Print Receipt", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            GoldDivider()

            // Paper size selector
            Text("Paper Size", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("3inch" to "3\" (58mm)", "4inch" to "4\" (80mm)").forEach { (value, label) ->
                    val selected = paperWidth == value
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(
                                if (selected) GoldPrimary.copy(0.2f) else BrownCard,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.5.dp,
                                if (selected) GoldPrimary else GoldDim.copy(0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                paperWidth = value
                                scope.launch { prefs.savePaperWidth(value) }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                null,
                                tint = if (selected) GoldPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = if (selected) GoldPrimary else TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Bluetooth printer selector
            Text("Select Printer", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            if (pairedDevices.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(StatusAmber.copy(0.1f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BluetoothDisabled, null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                    Text("No paired Bluetooth printers found. Please pair your printer in Android Settings.", color = StatusAmber, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pairedDevices) { device ->
                        @SuppressLint("MissingPermission")
                        val deviceName = try { device.name ?: device.address } catch (e: Exception) { device.address }
                        val isSelected = selectedDevice?.address == device.address
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(
                                    if (isSelected) GoldPrimary.copy(0.15f) else BrownCard,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, if (isSelected) GoldPrimary.copy(0.7f) else GoldDim.copy(0.2f), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedDevice = device
                                    scope.launch { prefs.savePrinterInfo(device.address, deviceName) }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, null, tint = if (isSelected) GoldPrimary else TextMuted, modifier = Modifier.size(20.dp))
                            Column(Modifier.weight(1f)) {
                                Text(deviceName, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                                Text(device.address, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Error / Success
            AnimatedVisibility(visible = errorMsg != null) {
                Row(Modifier.fillMaxWidth().background(StatusRed.copy(0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Icon(Icons.Default.Error, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(errorMsg ?: "", color = StatusRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            AnimatedVisibility(visible = successMsg != null) {
                Row(Modifier.fillMaxWidth().background(StatusGreen.copy(0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(successMsg ?: "", color = StatusGreen, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Print Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMsg = null
                        successMsg = null
                        repo.getReportHtml(reportName, recordId).fold(
                            onSuccess = { html ->
                                val widthMm = if (paperWidth == "4inch") 80 else 58
                                val wrappedHtml = ThermalPrintManager.wrapHtmlForPrint(html, widthMm)
                                ThermalPrintManager.printHtmlToBluetooth(context, wrappedHtml, widthMm, "TTS Receipt")
                                successMsg = "Print job sent! Check your printer."
                            },
                            onFailure = { err ->
                                errorMsg = "Failed: ${err.message}"
                            }
                        )
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(GoldBright, GoldPrimary))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = TextOnGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Fetching report...", color = TextOnGold, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Print, null, tint = TextOnGold)
                            Text("Print", style = MaterialTheme.typography.titleMedium, color = TextOnGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

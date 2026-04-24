package com.tts.fieldsales.ui.screen

import android.annotation.SuppressLint
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
import com.tts.fieldsales.data.api.OdooClient
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.print.BluetoothPrinterManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    var odooUrl by remember { mutableStateOf("") }
    var dbName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var printerName by remember { mutableStateOf("") }
    var paperWidth by remember { mutableStateOf("3inch") }
    var saveSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        odooUrl = prefs.odooUrl.first()
        dbName = prefs.dbName.first()
        username = prefs.username.first()
        printerName = prefs.printerName.first()
        paperWidth = prefs.paperWidth.first()
    }

    AppScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Settings
            GlassCard {
                SectionHeader("Odoo Server", icon = Icons.Default.Language)
                Spacer(Modifier.height(12.dp))
                GoldTextField(value = odooUrl, onValueChange = { odooUrl = it }, label = "Server URL", leadingIcon = Icons.Default.Language)
                Spacer(Modifier.height(10.dp))
                GoldTextField(value = dbName, onValueChange = { dbName = it }, label = "Database Name", leadingIcon = Icons.Default.Storage)
                Spacer(Modifier.height(10.dp))
                GoldTextField(value = username, onValueChange = { username = it }, label = "Username", leadingIcon = Icons.Default.Person)
                Spacer(Modifier.height(14.dp))
                GoldButton("Save Server Settings", onClick = {
                    scope.launch {
                        val pw = prefs.getPassword()
                        val uid = prefs.getUserId()
                        val uname = prefs.getUsername().ifBlank { username }
                        prefs.saveLoginInfo(odooUrl, dbName, username, pw, uid, uname)
                        OdooClient.initialize(odooUrl)
                        saveSuccess = true
                    }
                }, icon = Icons.Default.Save, modifier = Modifier.fillMaxWidth())
            }

            // Printer Settings
            GlassCard {
                SectionHeader("Thermal Printer", icon = Icons.Default.Print)
                Spacer(Modifier.height(12.dp))
                // Paper size
                Text("Paper Size", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("3inch" to "3\" (58mm)", "4inch" to "4\" (80mm)").forEach { (value, label) ->
                        val selected = paperWidth == value
                        Box(
                            Modifier.weight(1f)
                                .background(if (selected) GoldPrimary.copy(0.2f) else BrownCard, RoundedCornerShape(10.dp))
                                .border(1.5.dp, if (selected) GoldPrimary else GoldDim.copy(0.3f), RoundedCornerShape(10.dp))
                                .clickable { paperWidth = value; scope.launch { prefs.savePaperWidth(value) } }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = if (selected) GoldPrimary else TextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlineGoldButton("Setup Bluetooth Printer", onClick = onBack, icon = Icons.Default.Bluetooth, modifier = Modifier.fillMaxWidth())
                if (printerName.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                        Text("Connected: $printerName", color = StatusGreen, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // App Info
            GlassCard {
                SectionHeader("App Info", icon = Icons.Default.Info)
                Spacer(Modifier.height(10.dp))
                InfoRow("Version", "1.0.0", Icons.Default.NewReleases)
                InfoRow("App", "TTS Field Sales", Icons.Default.Store)
                InfoRow("Backend", "Odoo 19", Icons.Default.Cloud)
            }

            // Logout
            OutlinedButton(
                onClick = {
                    scope.launch {
                        prefs.logout()
                        OdooClient.clearCookies()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, StatusRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            if (saveSuccess) {
                Row(
                    Modifier.fillMaxWidth().background(StatusGreen.copy(0.1f), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                    Text("Settings saved!", color = StatusGreen)
                }
            }
        }
    }
}

@Composable
fun PrinterSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val btManager = remember { BluetoothPrinterManager(context) }
    val scope = rememberCoroutineScope()

    var pairedDevices by remember { mutableStateOf(listOf<android.bluetooth.BluetoothDevice>()) }
    var savedAddress by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        pairedDevices = btManager.getPairedPrinters()
        savedAddress = prefs.getPrinterAddress()
    }

    AppScaffold(title = "Bluetooth Printer", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!btManager.isBluetoothEnabled) {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.BluetoothDisabled, null, tint = StatusRed)
                        Text("Bluetooth is disabled. Please enable Bluetooth.", color = StatusRed)
                    }
                }
            } else if (pairedDevices.isEmpty()) {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.BluetoothSearching, null, tint = StatusAmber)
                        Text("No paired printers found. Pair your printer in Android Bluetooth Settings first.", color = StatusAmber)
                    }
                }
            } else {
                Text("Paired Bluetooth Devices", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pairedDevices) { device ->
                        @SuppressLint("MissingPermission")
                        val deviceName = try { device.name ?: device.address } catch (e: Exception) { device.address }
                        val isSelected = savedAddress == device.address
                        GlassCard(onClick = {
                            scope.launch {
                                prefs.savePrinterInfo(device.address, deviceName)
                                savedAddress = device.address
                                message = "Printer saved: $deviceName"
                            }
                        }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Bluetooth, null, tint = if (isSelected) GoldPrimary else TextMuted)
                                Column(Modifier.weight(1f)) {
                                    Text(deviceName, color = if (isSelected) TextPrimary else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Text(device.address, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = GoldPrimary)
                            }
                        }
                    }
                }
            }
            message?.let { Text(it, color = StatusGreen, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

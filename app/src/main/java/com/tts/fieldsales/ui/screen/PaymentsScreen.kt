package com.tts.fieldsales.ui.screen

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.PaymentsViewModel

// ─────────────────── PAYMENTS LIST ──────────────────────────────────────────

@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    onNewPayment: () -> Unit,
    viewModel: PaymentsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load(context) }

    AppScaffold(
        title = "Payments",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onNewPayment, containerColor = GoldPrimary, contentColor = TextOnGold, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "New Payment")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (state.isLoading) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(80.dp)) }
                }
            } else if (state.payments.isEmpty()) {
                EmptyState("No payments recorded", Icons.Default.Payment)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.payments, key = { it.id }) { payment ->
                        PaymentCard(payment = payment)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(payment: Payment) {
    var showPrint by remember { mutableStateOf(false) }
    val partnerName = payment.partnerId.toOdooName("Unknown")
    val journalName = payment.journalId.toOdooName("")

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(payment.fieldSalesRef ?: payment.name ?: "Receipt", style = MaterialTheme.typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    StatusBadge(payment.fieldSalesStatus ?: payment.state)
                }
                Text(partnerName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (journalName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccountBalance, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Text(journalName, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                    Text(payment.date ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SAR %.2f".format(payment.amount), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showPrint = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, "Print", tint = GoldDim, modifier = Modifier.size(18.dp))
                }
            }
        }
        // Official number badge
        if (payment.fieldSalesStatus == "approved" && payment.name != null) {
            GoldDivider(Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Verified, null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                Text("Official No: ${payment.name}", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
            }
        }
    }

    if (showPrint) {
        PrintBottomSheet(reportName = ThermalPrintManager.REPORT_PAYMENT, recordId = payment.id, onDismiss = { showPrint = false })
    }
}

// ─────────────────── NEW PAYMENT ─────────────────────────────────────────────

@Composable
fun NewPaymentScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var selectedCustomer by remember { mutableStateOf<Partner?>(null) }
    var selectedJournal by remember { mutableStateOf<Journal?>(null) }
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var customerSearch by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showJournalPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load(context) }

    state.actionMessage?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
            onBack()
        }
    }

    AppScaffold(title = "New Payment", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer picker
            GlassCard(onClick = { showCustomerPicker = true }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Customer", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            selectedCustomer?.name ?: "Tap to select customer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedCustomer != null) TextPrimary else TextMuted,
                            fontWeight = if (selectedCustomer != null) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = GoldDim)
                }
            }

            // Journal / Payment method
            GlassCard(onClick = { showJournalPicker = true }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            selectedJournal?.name ?: "Tap to select method",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedJournal != null) TextPrimary else TextMuted
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = GoldDim)
                }
            }

            // Amount
            GoldTextField(
                value = amount, onValueChange = { amount = it },
                label = "Amount (SAR)",
                leadingIcon = Icons.Default.AttachMoney,
                keyboardType = KeyboardType.Decimal
            )

            // Memo
            GoldTextField(
                value = memo, onValueChange = { memo = it },
                label = "Memo / Reference (optional)",
                leadingIcon = Icons.Default.Note,
                singleLine = false, maxLines = 3
            )

            // Info notice
            Row(
                Modifier.fillMaxWidth().background(StatusAmber.copy(0.1f), RoundedCornerShape(10.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = StatusAmber, modifier = Modifier.size(16.dp))
                Text("Payment will be submitted for approval before posting.", color = StatusAmber, style = MaterialTheme.typography.bodySmall)
            }

            // Action message
            AnimatedVisibility(visible = state.actionMessage != null) {
                Row(Modifier.fillMaxWidth().background(StatusGreen.copy(0.1f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.actionMessage ?: "", color = StatusGreen)
                }
            }

            GoldButton(
                text = "Record Payment",
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (selectedCustomer != null && selectedJournal != null && amt > 0) {
                        viewModel.createPayment(context, selectedCustomer!!.id, amt, selectedJournal!!.id, memo)
                    }
                },
                icon = Icons.Default.Payment,
                enabled = selectedCustomer != null && selectedJournal != null && amount.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Customer picker dialog
    if (showCustomerPicker) {
        PickerDialog(
            title = "Select Customer",
            items = state.customers.filter { it.name.contains(customerSearch, true) }.map { it.name to it },
            onSelect = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false },
            searchQuery = customerSearch,
            onSearchChange = { customerSearch = it }
        )
    }

    // Journal picker dialog
    if (showJournalPicker) {
        PickerDialog(
            title = "Select Payment Method",
            items = state.journals.map { it.name to it },
            onSelect = { selectedJournal = it; showJournalPicker = false },
            onDismiss = { showJournalPicker = false }
        )
    }
}

@Composable
private fun <T> PickerDialog(
    title: String,
    items: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String = "",
    onSearchChange: ((String) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BrownDark,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSearchChange != null) {
                    GoldSearchBar(query = searchQuery, onQueryChange = onSearchChange)
                }
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(items) { (label, item) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(item) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .background(BrownCard, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        GoldDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GoldPrimary) } }
    )
}

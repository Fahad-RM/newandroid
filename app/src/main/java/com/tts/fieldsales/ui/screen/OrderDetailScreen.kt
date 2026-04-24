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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.*

// ─────────────────── ORDER DETAIL ───────────────────────────────────────────

@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    onPreview: ((reportName: String, recordId: Int, recordName: String) -> Unit)? = null,
    onInvoiceDetail: ((Int) -> Unit)? = null,
    viewModel: OrderDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showPrint by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) { viewModel.load(context, orderId) }

    state.actionMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    AppScaffold(
        title = "",
        onBack = onBack
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (state.isLoading) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            } else {
                val order = state.order
                if (order == null) {
                    EmptyState("Order not found", Icons.Default.Error)
                } else {
                    Column(Modifier.fillMaxSize()) {
                        // Top Header Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(BrownMedium, BrownDark)), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                .padding(top = 16.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.background(GoldDim.copy(0.3f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("# ${order.name}", color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("GRAND TOTAL", style = MaterialTheme.typography.labelMedium, color = TextSecondary, letterSpacing = 1.5.sp)
                            Text("%.2f".format(order.amountTotal), style = MaterialTheme.typography.displayMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(order.partnerId.toOdooName(), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                            if ((order.warehouseId.toOdooName("")).isNotBlank()) {
                                Text(order.warehouseId.toOdooName(""), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            // Stepper
                            ApprovalStepper(state = order.state)
                        }
                        
                        // Items List
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text("Order Items", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                    Text("${state.lines.size} Items", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            itemsIndexed(state.lines.filter { it.displayType == null || it.displayType == "false" }) { index, line ->
                                OrderLineCard(index = index + 1, line = line)
                            }
                            item { Spacer(Modifier.height(80.dp)) } // Padding for bottom actions
                        }
                    }
                    
                    // Bottom Action Row
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, BrownDeep.copy(0.8f), BrownDeep))).padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Thermal
                            ActionButton(icon = Icons.Default.Print, label = "Thermal", onClick = { showPrint = true }, modifier = Modifier.weight(0.8f))
                            // Share
                            ActionButton(icon = Icons.Default.Share, label = "Share", onClick = { /* TODO */ }, modifier = Modifier.weight(0.8f))
                            
                            if (order.state == "draft") {
                                // Edit
                                ActionButton(icon = Icons.Default.Edit, label = "Edit", onClick = { /* TODO */ }, modifier = Modifier.weight(0.8f))
                                // Approve
                                Button(
                                    onClick = { viewModel.submitApproval(context, order.id) },
                                    modifier = Modifier.weight(1f).height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = StatusGreen, spotColor = StatusGreen),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Approve")
                                }
                            } else if (order.state == "waiting_approval") {
                                Box(modifier = Modifier.weight(1.8f).height(56.dp).background(StatusAmber.copy(0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("Pending Approval", color = StatusAmber, fontWeight = FontWeight.Bold)
                                }
                            } else if (order.state == "sale") {
                                order.invoiceIds?.firstOrNull()?.let { invoiceId ->
                                    Button(
                                        onClick = { onInvoiceDetail?.invoke(invoiceId) },
                                        modifier = Modifier.weight(1.8f).height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = GoldPrimary, spotColor = GoldPrimary),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("View Invoice")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrint) {
        val orderName = state.order?.name ?: "Order #$orderId"
        PrintBottomSheet(
            reportName = ThermalPrintManager.REPORT_SALE_ORDER,
            recordId = orderId,
            recordName = orderName,
            onDismiss = { showPrint = false },
            onPreview = onPreview?.let { navigate ->
                { navigate(ThermalPrintManager.REPORT_SALE_ORDER, orderId, orderName) }
            }
        )
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(BrownCard, RoundedCornerShape(12.dp))
            .border(1.dp, GoldDim.copy(0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ApprovalStepper(state: String) {
    val steps = listOf(
        Triple("Draft", "draft", Icons.Default.ShoppingCart),
        Triple("Pending", "waiting_approval", Icons.Default.Lock),
        Triple("Ordered", "sale", Icons.Default.Check)
    )
    val currentIdx = steps.indexOfFirst { it.second == state }.coerceAtLeast(0)
    
    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { idx, (label, _, icon) ->
            val isDone = idx <= currentIdx
            val isCurrent = idx == currentIdx
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isDone) GoldPrimary else BrownCard, CircleShape)
                        .border(2.dp, if (isDone) GoldPrimary else GoldDim.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (isDone) BrownDeep else TextMuted, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = if (isDone) TextPrimary else TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
            }
            if (idx < steps.size - 1) {
                Box(Modifier.weight(1f).height(2.dp).background(if (idx < currentIdx) GoldPrimary else GoldDim.copy(0.5f)))
            }
        }
    }
}

@Composable
private fun OrderLineCard(index: Int, line: SaleOrderLine) {
    val productName = line.productId.toOdooName("Product")
    val uomName = line.uomId.toOdooName("")
    val taxAmount = line.priceTotal - line.priceSubtotal
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrownCard, RoundedCornerShape(16.dp))
            .border(1.dp, GoldDim.copy(0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Text(index.toString(), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            
            Column(Modifier.weight(1f)) {
                Text(productName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${line.qty} Units @ %.2f".format(line.priceUnit), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    if ((line.discount ?: 0.0) > 0) {
                        Text(" (-${line.discount}%)", style = MaterialTheme.typography.labelSmall, color = StatusRed)
                    }
                }
                if (taxAmount > 0) {
                    Text("+ Tax: %.2f".format(taxAmount), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            
            Text("%.2f".format(line.priceTotal), style = MaterialTheme.typography.bodyLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────── INVOICES SCREEN ────────────────────────────────────────

@Composable
fun InvoicesScreen(
    onBack: () -> Unit,
    onInvoiceDetail: (Int) -> Unit,
    viewModel: InvoicesViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Invoices" to "out_invoice", "Credit Notes" to "out_refund")

    LaunchedEffect(selectedTab) { viewModel.load(context, tabs[selectedTab].second) }

    AppScaffold(title = "Invoices", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BrownDark,
                contentColor = GoldPrimary
            ) {
                tabs.forEachIndexed { idx, (label, _) ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx },
                        text = { Text(label, color = if (selectedTab == idx) GoldPrimary else TextSecondary) })
                }
            }

            if (state.isLoading) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(80.dp)) }
                }
            } else if (state.invoices.isEmpty()) {
                EmptyState("No records found", Icons.Default.Receipt)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceCard(invoice = invoice, onClick = { onInvoiceDetail(invoice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    val partnerName = invoice.partnerId.toOdooName("Unknown")
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(invoice.name ?: "Draft", style = MaterialTheme.typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    StatusBadge(invoice.state)
                }
                Text(partnerName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(invoice.invoiceDate ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SAR %.2f".format(invoice.amountTotal), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                if ((invoice.amountResidual ?: 0.0) > 0) {
                    Text("Due: %.2f".format(invoice.amountResidual), style = MaterialTheme.typography.labelSmall, color = StatusRed)
                }
            }
        }
    }
}

// ─────────────────── INVOICE DETAIL ─────────────────────────────────────────

@Composable
fun InvoiceDetailScreen(
    invoiceId: Int,
    onBack: () -> Unit,
    onPreview: ((reportName: String, recordId: Int, recordName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { com.tts.fieldsales.data.prefs.AppPreferences(context) }
    val repo = remember { com.tts.fieldsales.data.repository.OdooRepository(prefs) }
    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var lines by remember { mutableStateOf<List<InvoiceLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingLines by remember { mutableStateOf(true) }
    var showPrint by remember { mutableStateOf(false) }
    var linesError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(invoiceId) {
        isLoading = true
        repo.getInvoices().onSuccess { list ->
            invoice = list.firstOrNull { it.id == invoiceId }
            isLoading = false
        }.onFailure { e ->
            isLoading = false
        }
        isLoadingLines = true
        repo.getInvoiceLines(invoiceId).onSuccess { linesList ->
            lines = linesList
            linesError = null
            isLoadingLines = false
        }.onFailure { e ->
            linesError = e.message
            isLoadingLines = false
        }
    }

    AppScaffold(title = invoice?.name ?: "Invoice", onBack = onBack,
        actions = { IconButton(onClick = { showPrint = true }) { Icon(Icons.Default.Print, "Print", tint = GoldPrimary) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (isLoading) CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            else invoice?.let { inv ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        GlassCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    StatusBadge(inv.state)
                                    Spacer(Modifier.height(8.dp))
                                    StatusBadge(inv.paymentState ?: "not_paid")
                                }
                                Text("SAR %.2f".format(inv.amountTotal), style = MaterialTheme.typography.headlineMedium, color = GoldPrimary, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(12.dp))
                            InfoRow("Customer", inv.partnerId.toOdooName(), Icons.Default.Person)
                            InfoRow("Invoice Date", inv.invoiceDate ?: "-", Icons.Default.CalendarToday)
                            InfoRow("Due Date", inv.invoiceDateDue ?: "-", Icons.Default.Event)
                            InfoRow("Reference", inv.ref ?: "-", Icons.Default.Tag)
                        }
                    }
                    item {
                        GlassCard {
                            Text("Line Items", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            when {
                                isLoadingLines -> {
                                    repeat(3) {
                                        ShimmerBox(Modifier.fillMaxWidth().height(50.dp).padding(vertical = 4.dp))
                                    }
                                }
                                linesError != null -> {
                                    Text("Error loading lines: $linesError", color = StatusRed, style = MaterialTheme.typography.bodySmall)
                                }
                                lines.isEmpty() -> {
                                    Text("No line items found", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                                }
                                else -> {
                                    lines.forEachIndexed { idx, line ->
                                        if (idx > 0) GoldDivider(Modifier.padding(vertical = 4.dp))
                                        val name = line.productId.toOdooName(line.name ?: "-")
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(Modifier.weight(1f)) {
                                                Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                Text("${line.quantity} × %.2f".format(line.priceUnit), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Text("SAR %.2f".format(line.priceSubtotal), color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        GlassCard {
                            AmountDisplay("Subtotal", inv.amountUntaxed)
                            AmountDisplay("VAT (15%)", inv.amountTax)
                            GoldDivider(Modifier.padding(vertical = 6.dp))
                            AmountDisplay("Total", inv.amountTotal, isTotal = true)
                            if ((inv.amountResidual) > 0) {
                                Spacer(Modifier.height(4.dp))
                                AmountDisplay("Outstanding", inv.amountResidual)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrint) {
        val reportName = if (invoice?.moveType == "out_refund") ThermalPrintManager.REPORT_RETURN else ThermalPrintManager.REPORT_INVOICE
        val invName = invoice?.name ?: "Invoice #$invoiceId"
        PrintBottomSheet(
            reportName = reportName,
            recordId = invoiceId,
            recordName = invName,
            onDismiss = { showPrint = false },
            onPreview = onPreview?.let { navigate ->
                { navigate(reportName, invoiceId, invName) }
            }
        )
    }
}

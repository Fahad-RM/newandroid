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
        title = state.order?.name ?: "Order Detail",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showPrint = true }) {
                Icon(Icons.Default.Print, "Print", tint = GoldPrimary)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (state.isLoading) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            } else {
                val order = state.order
                if (order == null) {
                    EmptyState("Order not found", Icons.Default.Error)
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Status banner
                        item {
                            OrderStatusBanner(order = order, viewModel = viewModel, context = context)
                        }

                        // Info card
                        item {
                            GlassCard {
                                SectionHeader("Order Information")
                                Spacer(Modifier.height(12.dp))
                                InfoRow("Customer", (order.partnerId?.getOrNull(1) as? String) ?: "-", Icons.Default.Person)
                                InfoRow("Date", order.dateOrder?.take(16)?.replace("T", " ") ?: "-", Icons.Default.CalendarToday)
                                InfoRow("Warehouse", (order.warehouseId?.getOrNull(1) as? String) ?: "-", Icons.Default.Warehouse)
                                InfoRow("Status", order.state.replace("_", " "), Icons.Default.Info)
                            }
                        }

                        // Order lines
                        item {
                            GlassCard {
                                SectionHeader("Order Lines", "${state.lines.size} items")
                                Spacer(Modifier.height(12.dp))
                                state.lines.filter { it.displayType == null }.forEachIndexed { idx, line ->
                                    if (idx > 0) GoldDivider(Modifier.padding(vertical = 6.dp))
                                    OrderLineRow(line = line)
                                }
                            }
                        }

                        // Totals
                        item {
                            GlassCard {
                                AmountDisplay("Subtotal", order.amountUntaxed)
                                if (order.amountTax > 0) AmountDisplay("VAT (15%)", order.amountTax)
                                GoldDivider(Modifier.padding(vertical = 6.dp))
                                AmountDisplay("Total", order.amountTotal, isTotal = true)
                            }
                        }

                        // Action message
                        item {
                            AnimatedVisibility(visible = state.actionMessage != null) {
                                Row(
                                    Modifier.fillMaxWidth().background(StatusGreen.copy(0.12f), RoundedCornerShape(12.dp)).padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = StatusGreen)
                                    Text(state.actionMessage ?: "", color = StatusGreen, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        item { Spacer(Modifier.height(20.dp)) }
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
private fun OrderStatusBanner(order: SaleOrder, viewModel: OrderDetailViewModel, context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(BrownMedium, BrownCard)), RoundedCornerShape(16.dp))
            .border(1.dp, GoldDim.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatusBadge(order.state)
            Text("SAR %.2f".format(order.amountTotal), style = MaterialTheme.typography.headlineSmall, color = GoldPrimary, fontWeight = FontWeight.ExtraBold)
        }
        // Approval flow steps
        ApprovalProgressBar(state = order.state)
        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (order.state) {
                "draft" -> {
                    OutlineGoldButton("Submit Approval", onClick = { viewModel.submitApproval(context, order.id) }, modifier = Modifier.weight(1f), icon = Icons.Default.Send)
                    GoldButton("Confirm", onClick = { viewModel.confirmOrder(context, order.id) }, modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle)
                }
                "waiting_approval" -> {
                    Box(Modifier.fillMaxWidth().background(StatusAmber.copy(0.12f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HourglassEmpty, null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                            Text("Waiting for Manager Approval", color = StatusAmber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                "sale" -> GoldButton("Create Invoice", onClick = { viewModel.createInvoice(context, order.id) }, icon = Icons.Default.Receipt, modifier = Modifier.fillMaxWidth())
                "done" -> {}
            }
        }
    }
}

@Composable
private fun ApprovalProgressBar(state: String) {
    val steps = listOf("Draft" to "draft", "Approval" to "waiting_approval", "Confirmed" to "sale", "Done" to "done")
    val currentIdx = steps.indexOfFirst { it.second == state }.coerceAtLeast(0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { idx, (label, _) ->
            val isDone = idx <= currentIdx
            val isCurrent = idx == currentIdx
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(if (isCurrent) 10.dp else 8.dp)
                        .background(if (isDone) GoldPrimary else GoldDim.copy(0.3f), CircleShape)
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDone) GoldPrimary else TextMuted, maxLines = 1)
            }
            if (idx < steps.size - 1) {
                Box(Modifier.weight(0.5f).height(2.dp).background(if (idx < currentIdx) GoldPrimary else GoldDim.copy(0.3f), RoundedCornerShape(1.dp)))
            }
        }
    }
}

@Composable
private fun OrderLineRow(line: SaleOrderLine) {
    val productName = (line.productId?.getOrNull(1) as? String) ?: line.name ?: "Product"
    val uomName = (line.uomId?.getOrNull(1) as? String) ?: ""
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(productName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text("${line.qty} $uomName × SAR %.2f".format(line.priceUnit) + if ((line.discount ?: 0.0) > 0) " (${line.discount}% off)" else "",
                style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Text("SAR %.2f".format(line.priceSubtotal), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
    val partnerName = (invoice.partnerId?.getOrNull(1) as? String) ?: "Unknown"
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
    var showPrint by remember { mutableStateOf(false) }

    LaunchedEffect(invoiceId) {
        repo.getInvoices().onSuccess { list -> invoice = list.firstOrNull { it.id == invoiceId } }
        repo.getInvoiceLines(invoiceId).onSuccess { lines = it }
        isLoading = false
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
                            InfoRow("Customer", (inv.partnerId?.getOrNull(1) as? String) ?: "-", Icons.Default.Person)
                            InfoRow("Invoice Date", inv.invoiceDate ?: "-", Icons.Default.CalendarToday)
                            InfoRow("Due Date", inv.invoiceDateDue ?: "-", Icons.Default.Event)
                            InfoRow("Reference", inv.ref ?: "-", Icons.Default.Tag)
                        }
                    }
                    item {
                        GlassCard {
                            Text("Line Items", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            lines.forEachIndexed { idx, line ->
                                if (idx > 0) GoldDivider(Modifier.padding(vertical = 4.dp))
                                val name = (line.productId?.getOrNull(1) as? String) ?: line.name ?: "-"
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

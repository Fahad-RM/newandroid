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
import com.tts.fieldsales.data.model.SaleOrder
import com.tts.fieldsales.data.model.toOdooName
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.OrdersViewModel

@Composable
fun OrdersScreen(
    onNavigateBack: () -> Unit,
    onOrderDetail: (Int) -> Unit,
    onNewOrder: () -> Unit,
    viewModel: OrdersViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) { viewModel.load(context) }

    val filters = listOf("all" to "All", "draft" to "Draft", "waiting_approval" to "Pending", "sale" to "Confirmed", "done" to "Done")

    AppScaffold(
        title = "Sales Orders",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewOrder,
                containerColor = GoldPrimary,
                contentColor = TextOnGold,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "New Order") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
        ) {
            // Search + Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Search orders...")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(0.2f),
                                selectedLabelColor = GoldPrimary,
                                containerColor = BrownCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == key,
                                selectedBorderColor = GoldPrimary,
                                borderColor = GoldDim.copy(0.3f)
                            )
                        )
                    }
                }
            }

            if (state.isLoading) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(90.dp).padding(vertical = 4.dp)) }
                }
            } else {
                val filtered = state.orders.filter { order ->
                    (selectedFilter == "all" || order.state == selectedFilter) &&
                    (searchQuery.isBlank() || order.name.contains(searchQuery, ignoreCase = true) ||
                     (order.partnerId?.getOrNull(1) as? String)?.contains(searchQuery, ignoreCase = true) == true)
                }
                if (filtered.isEmpty()) {
                    EmptyState("No orders found", Icons.Default.ShoppingCart)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            OrderCard(order = order, onClick = { onOrderDetail(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: SaleOrder, onClick: () -> Unit) {
    val partnerName = order.partnerId.toOdooName("Unknown")
    val warehouseName = order.warehouseId.toOdooName("")

    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(order.name, style = MaterialTheme.typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    StatusBadge(order.state)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Person, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(partnerName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                }
                if (warehouseName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Warehouse, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Text(warehouseName, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
                Text(order.dateOrder?.take(10) ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SAR %.2f".format(order.amountTotal),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (order.amountTax > 0) {
                    Text("VAT: %.2f".format(order.amountTax), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
        // Invoice status
        if (order.invoiceStatus != null && order.invoiceStatus != "nothing") {
            GoldDivider(Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Receipt, null, tint = StatusBlue, modifier = Modifier.size(14.dp))
                Text(
                    when (order.invoiceStatus) {
                        "invoiced" -> "Fully Invoiced"
                        "to invoice" -> "Ready to Invoice"
                        else -> order.invoiceStatus.replace("_", " ")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusBlue
                )
            }
        }
    }
}

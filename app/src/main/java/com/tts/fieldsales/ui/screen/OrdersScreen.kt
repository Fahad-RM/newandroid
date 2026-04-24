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
    var searchCustomer by remember { mutableStateOf("") }
    var searchOrderRef by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) { viewModel.load(context) }

    val filters = listOf("All", "Approved", "Not Approved")

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
                GoldSearchBar(query = searchCustomer, onQueryChange = { searchCustomer = it }, placeholder = "Search Customer...")
                GoldSearchBar(query = searchOrderRef, onQueryChange = { searchOrderRef = it }, placeholder = "Search Order Ref...")
                
                // Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrownCardElevated, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(if (isSelected) BrownCard else Color.Transparent, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) GoldDim.copy(0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedFilter = filter },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                filter,
                                color = if (isSelected) GoldPrimary else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(90.dp).padding(vertical = 4.dp)) }
                }
            } else {
                val filtered = state.orders.filter { order ->
                    val statusMatch = when (selectedFilter) {
                        "Approved" -> order.state == "sale" || order.state == "done"
                        "Not Approved" -> order.state == "draft" || order.state == "sent" || order.state == "waiting_approval"
                        else -> true
                    }
                    statusMatch &&
                    (searchCustomer.isBlank() || (order.partnerId.toOdooName()).contains(searchCustomer, ignoreCase = true)) &&
                    (searchOrderRef.isBlank() || order.name.contains(searchOrderRef, ignoreCase = true))
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
    val isDraft = order.state == "draft"
    val stripColor = if (isDraft) StatusRed else StatusGreen

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GoldDim.copy(0.2f), spotColor = GoldDim.copy(0.2f))
            .background(BrownCard, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Colored side strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .background(stripColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .align(Alignment.CenterStart)
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Ref and Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Ref Pill
                Box(modifier = Modifier.background(StatusBlue.copy(0.15f), RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("# ${order.name}", color = StatusBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                // Status Pill
                Box(modifier = Modifier.background(stripColor.copy(0.15f), RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = stripColor, modifier = Modifier.size(12.dp))
                        Text(order.state.uppercase(), color = stripColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text(partnerName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            if (warehouseName.isNotBlank()) {
                Text(warehouseName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CalendarToday, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Text(order.dateOrder ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            
            Spacer(Modifier.height(12.dp))
            GoldDivider()
            Spacer(Modifier.height(12.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TOTAL AMOUNT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("%.2f".format(order.amountTotal), style = MaterialTheme.typography.titleLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Action Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                    border = BorderStroke(1.dp, StatusRed.copy(0.5f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}

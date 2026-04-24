package com.tts.fieldsales.ui.screen

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
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NewOrderScreen(onBack: () -> Unit, onOrderCreated: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    val scope = rememberCoroutineScope()

    var customers by remember { mutableStateOf<List<Partner>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var warehouses by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedCustomer by remember { mutableStateOf<Partner?>(null) }
    var selectedWarehouseId by remember { mutableStateOf<Int?>(null) }
    var selectedWarehouseName by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var productSearch by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showWarehousePicker by remember { mutableStateOf(false) }
    var showProductSearch by remember { mutableStateOf(false) }
    var customerSearch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repo.getCustomers().onSuccess { customers = it }
        repo.getWarehouses().onSuccess { whs ->
            warehouses = whs
            whs.firstOrNull()?.let { wh ->
                selectedWarehouseId = (wh["id"] as? Double)?.toInt()
                selectedWarehouseName = wh["name"] as? String ?: ""
            }
        }
        repo.getProducts().onSuccess { products = it }
    }

    val subtotal = cartItems.sumOf { it.subtotal }
    val vat = subtotal * 0.15
    val total = subtotal + vat

    AppScaffold(title = "New Order", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
        ) {
            // Form section (scrollable top)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Customer picker
                PickerCard(
                    label = "Customer",
                    value = selectedCustomer?.name ?: "Select customer",
                    icon = Icons.Default.Person,
                    onClick = { showCustomerPicker = true }
                )

                // Warehouse
                PickerCard(
                    label = "Warehouse",
                    value = selectedWarehouseName.ifBlank { "Select warehouse" },
                    icon = Icons.Default.Warehouse,
                    onClick = { showWarehousePicker = true }
                )

                // Products section
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Order Lines", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showProductSearch = true }) {
                            Icon(Icons.Default.AddCircle, null, tint = GoldPrimary)
                        }
                    }
                    if (cartItems.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("No items added. Tap + to add products.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        cartItems.forEachIndexed { idx, item ->
                            if (idx > 0) GoldDivider(Modifier.padding(vertical = 6.dp))
                            CartItemRow(
                                item = item,
                                onQtyChange = { newQty ->
                                    cartItems = cartItems.toMutableList().also { it[idx] = item.copy(qty = newQty) }
                                },
                                onRemove = {
                                    cartItems = cartItems.toMutableList().also { it.removeAt(idx) }
                                }
                            )
                        }
                    }
                }

                // Totals
                if (cartItems.isNotEmpty()) {
                    GlassCard {
                        AmountDisplay("Subtotal", subtotal)
                        AmountDisplay("VAT (15%)", vat)
                        GoldDivider(Modifier.padding(vertical = 6.dp))
                        AmountDisplay("Total", total, isTotal = true)
                    }
                }

                errorMsg?.let {
                    Text(it, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Bottom action bar
            Column(
                Modifier.background(BrownDark).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlineGoldButton(
                        "Save Draft",
                        onClick = {
                            scope.launch {
                                if (selectedCustomer == null || cartItems.isEmpty()) {
                                    errorMsg = "Select customer and add products"
                                    return@launch
                                }
                                isLoading = true
                                val lines = cartItems.map { mapOf("product_id" to it.productId, "product_uom_qty" to it.qty, "price_unit" to it.priceUnit, "discount" to it.discount, "product_uom" to it.uomId) }
                                repo.createSaleOrder(selectedCustomer!!.id, selectedWarehouseId ?: 1, lines).fold(
                                    onSuccess = { id -> onOrderCreated(id) },
                                    onFailure = { errorMsg = it.message }
                                )
                                isLoading = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GoldButton(
                        "Submit for Approval",
                        onClick = {
                            scope.launch {
                                if (selectedCustomer == null || cartItems.isEmpty()) {
                                    errorMsg = "Select customer and add products"
                                    return@launch
                                }
                                isLoading = true
                                val lines = cartItems.map { mapOf("product_id" to it.productId, "product_uom_qty" to it.qty, "price_unit" to it.priceUnit, "discount" to it.discount, "product_uom" to it.uomId) }
                                repo.createSaleOrder(selectedCustomer!!.id, selectedWarehouseId ?: 1, lines).fold(
                                    onSuccess = { id ->
                                        repo.submitOrderForApproval(id)
                                        onOrderCreated(id)
                                    },
                                    onFailure = { errorMsg = it.message }
                                )
                                isLoading = false
                            }
                        },
                        loading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Product search bottom sheet
    if (showProductSearch) {
        ProductSearchSheet(
            products = products,
            search = productSearch,
            onSearchChange = { productSearch = it },
            onSelect = { product ->
                val uomId = (product.uomId?.getOrNull(0) as? Double)?.toInt() ?: 1
                val uomName = (product.uomId?.getOrNull(1) as? String) ?: "Unit"
                val existingIdx = cartItems.indexOfFirst { it.productId == product.id }
                cartItems = if (existingIdx >= 0) {
                    cartItems.toMutableList().also { it[existingIdx] = it[existingIdx].copy(qty = it[existingIdx].qty + 1) }
                } else {
                    cartItems + CartItem(product.id, product.name, product.image128, uomId, uomName, 1.0, product.listPrice, 0.0, product.taxIds ?: emptyList())
                }
                showProductSearch = false
            },
            onDismiss = { showProductSearch = false }
        )
    }

    // Customer picker
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            containerColor = BrownDark,
            title = { Text("Select Customer", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldSearchBar(query = customerSearch, onQueryChange = { customerSearch = it })
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        val filtered = customers.filter { it.name.contains(customerSearch, true) }
                        items(filtered) { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { selectedCustomer = c; showCustomerPicker = false }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(Modifier.size(36.dp).background(GoldPrimary.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(c.name.first().toString(), color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(c.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    if (!c.ref.isNullOrBlank()) Text("Ref: ${c.ref}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            GoldDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCustomerPicker = false }) { Text("Cancel", color = GoldPrimary) } }
        )
    }

    // Warehouse picker
    if (showWarehousePicker) {
        AlertDialog(
            onDismissRequest = { showWarehousePicker = false },
            containerColor = BrownDark,
            title = { Text("Select Warehouse", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(warehouses) { wh ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selectedWarehouseId = (wh["id"] as? Double)?.toInt()
                                selectedWarehouseName = wh["name"] as? String ?: ""
                                showWarehousePicker = false
                            }.padding(12.dp)
                        ) { Text(wh["name"] as? String ?: "-", color = TextPrimary) }
                        GoldDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWarehousePicker = false }) { Text("Cancel", color = GoldPrimary) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSearchSheet(
    products: List<Product>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BrownDark) {
        Column(Modifier.padding(16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Product", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            GoldSearchBar(query = search, onQueryChange = onSearchChange, placeholder = "Search products...")
            val filtered = products.filter { it.name.contains(search, true) || it.defaultCode?.contains(search, true) == true }
            LazyColumn(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { product ->
                    GlassCard(onClick = { onSelect(product) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp).background(GoldPrimary.copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory2, null, tint = GoldDim)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(product.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (!product.defaultCode.isNullOrBlank()) Text("[${product.defaultCode}]", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("SAR %.2f".format(product.listPrice), color = GoldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onQtyChange: (Double) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(item.productName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("SAR %.2f × %.0f = SAR %.2f".format(item.priceUnit, item.qty, item.subtotal), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (item.qty > 1) onQtyChange(item.qty - 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, null, tint = GoldDim, modifier = Modifier.size(16.dp))
            }
            Text("%.0f".format(item.qty), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = { onQtyChange(item.qty + 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, null, tint = StatusRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PickerCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = GoldDim, modifier = Modifier.size(20.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = GoldDim)
        }
    }
}

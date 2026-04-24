package com.tts.fieldsales.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showWarehousePicker by remember { mutableStateOf(false) }
    var customerSearch by remember { mutableStateOf("") }

    // State for Split View
    var isProductGridVisible by remember { mutableStateOf(true) }
    var selectedBrandId by remember { mutableStateOf<Int?>(null) }
    var productSearch by remember { mutableStateOf("") }
    var showVariantPopupFor by remember { mutableStateOf<List<Product>?>(null) }

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

    // Group Products by Template for Variants
    val groupedProducts = remember(products, productSearch, selectedBrandId) {
        products
            .filter { 
                (selectedBrandId == null || it.brandId.toOdooId() == selectedBrandId) &&
                (productSearch.isBlank() || it.name.contains(productSearch, true) || it.defaultCode?.contains(productSearch, true) == true)
            }
            .groupBy { it.productTmplId.toOdooId() ?: it.id }
            .values.toList()
    }

    val uniqueBrands = remember(products) {
        products.mapNotNull { 
            val id = it.brandId.toOdooId()
            val name = it.brandId.toOdooName()
            if (id != null && name != "-") id to name else null
        }.distinctBy { it.first }
    }

    fun addToCart(product: Product, qty: Double = 1.0) {
        val uomId = product.uomId.toOdooId() ?: 1
        val uomName = product.uomId.toOdooName("Unit")
        val existingIdx = cartItems.indexOfFirst { it.productId == product.id }
        cartItems = if (existingIdx >= 0) {
            cartItems.toMutableList().also { it[existingIdx] = it[existingIdx].copy(qty = it[existingIdx].qty + qty) }
        } else {
            cartItems + CartItem(product.id, product.name, product.image128, uomId, uomName, qty, product.listPrice, 0.0, product.taxIds ?: emptyList())
        }
    }

    AppScaffold(title = "New Order", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
        ) {
            
            // TOP HALF: Cart & Info
            Column(
                Modifier.fillMaxWidth()
                    .weight(if (isProductGridVisible) 0.45f else 1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selectors (compact row)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        PickerCard(label = "Customer", value = selectedCustomer?.name ?: "Select", icon = Icons.Default.Person, onClick = { showCustomerPicker = true })
                    }
                    Box(Modifier.weight(1f)) {
                        PickerCard(label = "Warehouse", value = selectedWarehouseName.ifBlank { "Select" }, icon = Icons.Default.Warehouse, onClick = { showWarehousePicker = true })
                    }
                }

                // Cart List
                GlassCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (cartItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cart is empty", color = TextMuted)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(cartItems) { idx, item ->
                                if (idx > 0) GoldDivider(Modifier.padding(vertical = 4.dp))
                                CartItemRow(
                                    item = item,
                                    onQtyChange = { newQty -> cartItems = cartItems.toMutableList().also { it[idx] = item.copy(qty = newQty) } },
                                    onRemove = { cartItems = cartItems.toMutableList().also { it.removeAt(idx) } }
                                )
                            }
                        }
                    }
                }

                // Totals
                if (cartItems.isNotEmpty()) {
                    GlassCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("Subtotal: SAR %.2f".format(subtotal), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                Text("VAT: SAR %.2f".format(vat), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("SAR %.2f".format(total), color = GoldPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // MIDDLE TABS / TOGGLE
            Row(
                Modifier.fillMaxWidth().background(BrownDark.copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Products", 
                    color = TextPrimary, 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { isProductGridVisible = !isProductGridVisible }) {
                    Text(if (isProductGridVisible) "Hide Products" else "Show Products", color = GoldPrimary)
                    Icon(if (isProductGridVisible) Icons.Default.ExpandMore else Icons.Default.ExpandLess, null, tint = GoldPrimary)
                }
            }

            // BOTTOM HALF: Product Grid
            AnimatedVisibility(
                visible = isProductGridVisible,
                enter = slideInVertically { it } + expandVertically(),
                exit = slideOutVertically { it } + shrinkVertically(),
                modifier = Modifier.weight(0.55f).fillMaxWidth()
            ) {
                Column(Modifier.fillMaxSize().background(BrownDarkest)) {
                    // Search & Brand Filter
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldSearchBar(query = productSearch, onQueryChange = { productSearch = it }, placeholder = "Search products...")
                        
                        // Brands
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedBrandId == null,
                                    onClick = { selectedBrandId = null },
                                    label = { Text("All Brands") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary.copy(0.2f), selectedLabelColor = GoldPrimary)
                                )
                            }
                            items(uniqueBrands) { (id, name) ->
                                FilterChip(
                                    selected = selectedBrandId == id,
                                    onClick = { selectedBrandId = id },
                                    label = { Text(name) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary.copy(0.2f), selectedLabelColor = GoldPrimary)
                                )
                            }
                        }
                    }

                    // Product Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(groupedProducts.size) { idx ->
                            val variants = groupedProducts[idx]
                            val displayProd = variants.first()
                            ProductGridCard(
                                product = displayProd,
                                variantCount = if (variants.size > 1) variants.size else 0,
                                onClick = {
                                    if (variants.size == 1) {
                                        addToCart(variants.first())
                                    } else {
                                        showVariantPopupFor = variants
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom action bar (Always visible)
            Column(
                Modifier.background(BrownDark).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMsg?.let { Text(it, color = StatusRed, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlineGoldButton(
                        "Save Draft",
                        onClick = {
                            scope.launch {
                                if (selectedCustomer == null || cartItems.isEmpty()) { errorMsg = "Select customer and add products"; return@launch }
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
                        "Submit",
                        onClick = {
                            scope.launch {
                                if (selectedCustomer == null || cartItems.isEmpty()) { errorMsg = "Select customer and add products"; return@launch }
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

    // Popups
    if (showVariantPopupFor != null) {
        VariantSelectionSheet(
            variants = showVariantPopupFor!!,
            onAdd = { prod, qty -> addToCart(prod, qty); showVariantPopupFor = null },
            onDismiss = { showVariantPopupFor = null }
        )
    }

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

@Composable
private fun PickerCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = GoldDim, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ProductGridCard(product: Product, variantCount: Int, onClick: () -> Unit) {
    GlassCard(onClick = onClick, modifier = Modifier.aspectRatio(0.7f).padding(2.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.05f))) {
                if (!product.image128.isNullOrBlank()) {
                    val bitmap = remember(product.image128) { base64ToBitmap(product.image128) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Inventory2, null, tint = GoldDim, modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                } else {
                    Icon(Icons.Default.Inventory2, null, tint = GoldDim, modifier = Modifier.align(Alignment.Center).size(32.dp))
                }
                if (variantCount > 0) {
                    Box(Modifier.align(Alignment.TopEnd).background(GoldPrimary, RoundedCornerShape(bottomStart = 8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("+$variantCount", color = BrownDarkest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(product.name, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text("SAR %.2f".format(product.listPrice), color = GoldPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantSelectionSheet(variants: List<Product>, onAdd: (Product, Double) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BrownDark) {
        Column(Modifier.padding(16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Select Variant", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(variants) { prod ->
                    GlassCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(prod.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("SAR %.2f".format(prod.listPrice), color = GoldPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            GoldButton("Add", onClick = { onAdd(prod, 1.0) })
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
            Text(item.productName, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("SAR %.2f × %.0f = SAR %.2f".format(item.priceUnit, item.qty, item.subtotal), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (item.qty > 1) onQtyChange(item.qty - 1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, null, tint = GoldDim, modifier = Modifier.size(14.dp))
            }
            Text("%.0f".format(item.qty), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { onQtyChange(item.qty + 1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.DeleteOutline, null, tint = StatusRed, modifier = Modifier.size(16.dp))
        }
    }
}

private fun base64ToBitmap(base64Str: String): ImageBitmap? {
    return try {
        // Strip out the data URL prefix if present
        val cleanStr = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

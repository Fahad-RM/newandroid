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
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.launch

// ─────────────────── RETURNS ─────────────────────────────────────────────────

@Composable
fun ReturnsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    var returns by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewReturn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repo.getReturns().onSuccess { returns = it }
        isLoading = false
    }

    AppScaffold(
        title = "Returns / Credit Notes",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewReturn = true }, containerColor = GoldPrimary, contentColor = TextOnGold, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "New Return")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (isLoading) CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            else if (returns.isEmpty()) EmptyState("No returns found", Icons.Default.AssignmentReturn)
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(returns, key = { it.id }) { ret ->
                    var showPrint by remember { mutableStateOf(false) }
                    GlassCard(onClick = { showPrint = true }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(ret.name ?: "Draft", color = GoldPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    StatusBadge(ret.state)
                                }
                                Text(ret.partnerId.toOdooName("-"), color = TextSecondary)
                                Text(ret.invoiceDate ?: "-", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SAR %.2f".format(ret.amountTotal), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Icon(Icons.Default.Print, null, tint = GoldDim, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (showPrint) PrintBottomSheet(reportName = ThermalPrintManager.REPORT_RETURN, recordId = ret.id, onDismiss = { showPrint = false })
                }
            }
        }
    }

    if (showNewReturn) NewReturnSheet(repo = repo, onDismiss = { showNewReturn = false }, onCreated = { LaunchedEffect(Unit) { repo.getReturns().onSuccess { returns = it } } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewReturnSheet(repo: OdooRepository, onDismiss: () -> Unit, onCreated: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var customers by remember { mutableStateOf<List<Partner>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedCustomer by remember { mutableStateOf<Partner?>(null) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repo.getCustomers().onSuccess { customers = it }
        repo.getProducts().onSuccess { products = it }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BrownDark) {
        Column(Modifier.padding(20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("New Return / Credit Note", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            GoldDivider()

            // Customer
            GlassCard {
                Text("Customer", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text(selectedCustomer?.name ?: "Select customer", color = TextPrimary)
            }

            // Info
            Row(Modifier.fillMaxWidth().background(StatusAmber.copy(0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                Icon(Icons.Default.Info, null, tint = StatusAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Return will be submitted for manager approval before processing.", color = StatusAmber, style = MaterialTheme.typography.bodySmall)
            }

            msg?.let { Text(it, color = if (it.contains("success")) StatusGreen else StatusRed) }

            GoldButton("Create Return", onClick = {
                if (selectedCustomer == null || cartItems.isEmpty()) {
                    msg = "Select customer and add products"
                    return@GoldButton
                }
                scope.launch {
                    isLoading = true
                    val lines = cartItems.map { mapOf("product_id" to it.productId, "quantity" to it.qty, "price_unit" to it.priceUnit) }
                    repo.createReturn(selectedCustomer!!.id, lines).fold(
                        onSuccess = { msg = "Return created successfully!"; kotlinx.coroutines.delay(1500); onDismiss() },
                        onFailure = { msg = "Failed: ${it.message}" }
                    )
                    isLoading = false
                }
            }, loading = isLoading, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─────────────────── CUSTOMER DETAIL ─────────────────────────────────────────

@Composable
fun CustomerDetailScreen(customerId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    var customer by remember { mutableStateOf<Partner?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showPrint by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        repo.getCustomers().onSuccess { list -> customer = list.firstOrNull { it.id == customerId } }
        isLoading = false
    }

    AppScaffold(
        title = customer?.name ?: "Customer",
        onBack = onBack,
        actions = { IconButton(onClick = { showPrint = true }) { Icon(Icons.Default.Print, null, tint = GoldPrimary) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (isLoading) CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            else customer?.let { c ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        GoldenCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(64.dp).background(GoldPrimary.copy(0.15f), CircleShape).border(2.dp, GoldPrimary, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(c.name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineMedium, color = GoldPrimary, fontWeight = FontWeight.ExtraBold)
                                }
                                Column {
                                    Text(c.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                                    if (!c.arabicName.isNullOrBlank()) Text(c.arabicName, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    if (!c.ref.isNullOrBlank()) Text("Ref: ${c.ref}", color = GoldDim, style = MaterialTheme.typography.labelMedium)
                                    StatusBadge(c.approvalStatus ?: "approved")
                                }
                            }
                        }
                    }
                    item {
                        GlassCard {
                            SectionHeader("Contact Information")
                            Spacer(Modifier.height(10.dp))
                            InfoRow("Phone", c.phone ?: "-", Icons.Default.Phone)
                            InfoRow("Mobile", c.mobile ?: "-", Icons.Default.PhoneAndroid)
                            InfoRow("Email", c.email ?: "-", Icons.Default.Email)
                            InfoRow("Address", "${c.street ?: ""} ${c.city ?: ""}".trim(), Icons.Default.LocationOn)
                            InfoRow("VAT", c.vat ?: "-", Icons.Default.Receipt)
                        }
                    }
                    item {
                        GlassCard {
                            SectionHeader("Financial Info")
                            Spacer(Modifier.height(10.dp))
                            AmountDisplay("Outstanding Balance", c.credit ?: 0.0)
                            AmountDisplay("Credit Limit", c.creditLimit ?: 0.0)
                            AmountDisplay("Overdue Amount", c.totalOverdue ?: 0.0)
                        }
                    }
                    item {
                        OutlineGoldButton("View Statement", onClick = {}, icon = Icons.Default.Receipt, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    if (showPrint) {
        PrintBottomSheet(reportName = ThermalPrintManager.REPORT_STATEMENT, recordId = customerId, onDismiss = { showPrint = false })
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
}

// ─────────────────── NEW CUSTOMER ────────────────────────────────────────────

@Composable
fun NewCustomerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var arabicName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    AppScaffold(title = "New Customer", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth().background(StatusAmber.copy(0.1f), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, null, tint = StatusAmber, modifier = Modifier.size(16.dp))
                Text("New customer will be submitted for approval.", color = StatusAmber, style = MaterialTheme.typography.bodySmall)
            }
            GoldTextField(value = name, onValueChange = { name = it }, label = "Customer Name *", leadingIcon = Icons.Default.Person)
            GoldTextField(value = arabicName, onValueChange = { arabicName = it }, label = "Arabic Name", leadingIcon = Icons.Default.Translate)
            GoldTextField(value = phone, onValueChange = { phone = it }, label = "Phone", leadingIcon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
            GoldTextField(value = mobile, onValueChange = { mobile = it }, label = "Mobile", leadingIcon = Icons.Default.PhoneAndroid, keyboardType = KeyboardType.Phone)
            GoldTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Icons.Default.Email, keyboardType = KeyboardType.Email)
            GoldTextField(value = vat, onValueChange = { vat = it }, label = "VAT Number", leadingIcon = Icons.Default.Receipt)
            GoldTextField(value = street, onValueChange = { street = it }, label = "Street Address", leadingIcon = Icons.Default.LocationOn)
            GoldTextField(value = city, onValueChange = { city = it }, label = "City", leadingIcon = Icons.Default.LocationCity)

            msg?.let { Text(it, color = if (it.contains("success", true)) StatusGreen else StatusRed, style = MaterialTheme.typography.bodySmall) }

            GoldButton("Create Customer", onClick = {
                if (name.isBlank()) { msg = "Name is required"; return@GoldButton }
                scope.launch {
                    isLoading = true
                    val vals = buildMap {
                        put("name", name); put("is_company", true)
                        if (arabicName.isNotBlank()) put("arabic_customer_name", arabicName)
                        if (phone.isNotBlank()) put("phone", phone)
                        if (mobile.isNotBlank()) put("mobile", mobile)
                        if (email.isNotBlank()) put("email", email)
                        if (vat.isNotBlank()) put("vat", vat)
                        if (street.isNotBlank()) put("street", street)
                        if (city.isNotBlank()) put("city", city)
                    }
                    repo.createCustomer(vals).fold(
                        onSuccess = { msg = "Customer created successfully!"; kotlinx.coroutines.delay(1500); onBack() },
                        onFailure = { msg = "Failed: ${it.message}" }
                    )
                    isLoading = false
                }
            }, loading = isLoading, icon = Icons.Default.PersonAdd, modifier = Modifier.fillMaxWidth())
        }
    }
}

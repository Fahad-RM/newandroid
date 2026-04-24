package com.tts.fieldsales.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.*

// ─────────────────── CUSTOMERS LIST ──────────────────────────────────────────

@Composable
fun CustomersScreen(
    onBack: () -> Unit,
    onCustomerDetail: (Int) -> Unit,
    onNewCustomer: () -> Unit,
    viewModel: CustomersViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load(context) }

    AppScaffold(
        title = "Customers",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCustomer, containerColor = GoldPrimary, contentColor = TextOnGold, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.PersonAdd, "New Customer")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            Box(Modifier.padding(16.dp)) {
                GoldSearchBar(query = searchQuery, onQueryChange = { searchQuery = it; viewModel.search(context, it) }, placeholder = "Search customers...")
            }
            if (state.isLoading) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(70.dp)) }
                }
            } else {
                val filtered = state.customers.filter { it.name.contains(searchQuery, true) || it.ref?.contains(searchQuery, true) == true }
                if (filtered.isEmpty()) EmptyState("No customers found", Icons.Default.People)
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { customer ->
                        CustomerCard(customer = customer, onClick = { onCustomerDetail(customer.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: Partner, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(48.dp).background(GoldPrimary.copy(0.15f), CircleShape).border(1.dp, GoldDim.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(customer.name.first().uppercaseChar().toString(), style = MaterialTheme.typography.titleLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(customer.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    if (customer.approvalStatus == "draft") StatusBadge("waiting")
                }
                if (!customer.ref.isNullOrBlank()) Text("Ref: ${customer.ref}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                if (!customer.city.isNullOrBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Text(customer.city, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            if ((customer.credit ?: 0.0) > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("SAR %.0f".format(customer.credit), color = StatusRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Balance", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─────────────────── ROUTE / VISITS ──────────────────────────────────────────

@Composable
fun RoutesScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var visitNotes by remember { mutableStateOf("") }
    var showEndDialog by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.load(context) }

    AppScaffold(title = "Today's Route", onBack = onBack,
        actions = { IconButton(onClick = { viewModel.load(context) }) { Icon(Icons.Default.Refresh, "Refresh", tint = GoldPrimary) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (state.isLoading) CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            else {
                val route = state.route
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Route summary
                    route?.let { r ->
                        item {
                            GoldenCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(r.dayOfWeek ?: "Today", style = MaterialTheme.typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                        Text(r.date ?: "-", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    StatusBadge(r.state ?: "pending")
                                }
                                Spacer(Modifier.height(12.dp))
                                // Progress bar
                                val progress = ((r.completedVisits ?: 0).toFloat() / (r.totalVisits?.takeIf { it > 0 } ?: 1).toFloat())
                                val progressAnim by animateFloatAsState(progress, tween(800), label = "route_progress")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${r.completedVisits}/${r.totalVisits} visits", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("${r.totalDistance} km", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progressAnim },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = GoldPrimary, trackColor = GoldDim.copy(0.2f)
                                )
                            }
                        }
                        // Visit items
                        items(r.visits ?: emptyList()) { visit ->
                            VisitCard(
                                visit = visit,
                                onStart = { viewModel.startVisit(context, visit.id, 0.0, 0.0) },
                                onEnd = { showEndDialog = visit.id }
                            )
                        }
                    }
                    if (route?.visits.isNullOrEmpty()) {
                        item { EmptyState("No visits scheduled for today", Icons.Default.Map) }
                    }
                }
            }
        }
    }

    // End visit dialog
    showEndDialog?.let { visitId ->
        AlertDialog(
            onDismissRequest = { showEndDialog = null },
            containerColor = BrownDark,
            title = { Text("End Visit", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add notes about this visit:", color = TextSecondary)
                    GoldTextField(value = visitNotes, onValueChange = { visitNotes = it }, label = "Visit Notes", singleLine = false, maxLines = 4)
                }
            },
            confirmButton = {
                GoldButton("Complete Visit", onClick = {
                    viewModel.endVisit(context, visitId, 0.0, 0.0, visitNotes)
                    showEndDialog = null
                    visitNotes = ""
                })
            },
            dismissButton = { TextButton(onClick = { showEndDialog = null }) { Text("Cancel", color = TextMuted) } }
        )
    }
}

@Composable
private fun VisitCard(visit: VisitData, onStart: () -> Unit, onEnd: () -> Unit) {
    val stateColor = when (visit.state) {
        "in_progress" -> StatusBlue
        "completed", "visited" -> StatusGreen
        "skipped" -> StatusAmber
        else -> TextMuted
    }

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(stateColor.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (visit.state) {
                            "completed", "visited" -> Icons.Default.CheckCircle
                            "in_progress" -> Icons.Default.PlayCircle
                            "skipped" -> Icons.Default.SkipNext
                            else -> Icons.Default.RadioButtonUnchecked
                        },
                        null, tint = stateColor, modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(visit.customerName ?: "Unknown", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(visit.address?.take(50) ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 2)
                    if (visit.distanceFromUser != null && visit.distanceFromUser > 0) {
                        Text("%.1f km away".format(visit.distanceFromUser), style = MaterialTheme.typography.labelSmall, color = GoldDim)
                    }
                }
            }
            when (visit.state) {
                "scheduled", "draft", "planned" -> {
                    SmallGoldButton("Start") { onStart() }
                }
                "in_progress" -> {
                    SmallGoldButton("End") { onEnd() }
                }
                else -> StatusBadge(visit.state ?: "")
            }
        }
        if (!visit.notes.isNullOrBlank()) {
            GoldDivider(Modifier.padding(vertical = 6.dp))
            Text(visit.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SmallGoldButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold)
    ) { Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

// ─────────────────── ATTENDANCE ──────────────────────────────────────────────

@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isCheckedIn = state.status == "checked_in"

    LaunchedEffect(Unit) { viewModel.load(context) }

    AppScaffold(title = "Attendance", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Status ring
            val ringColor = if (isCheckedIn) StatusGreen else StatusAmber
            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = ringColor.copy(0.15f))
                    drawCircle(color = ringColor.copy(0.3f), radius = size.minDimension * 0.45f, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (isCheckedIn) Icons.Default.Fingerprint else Icons.Default.PersonOff,
                        null, tint = ringColor, modifier = Modifier.size(52.dp)
                    )
                    Text(
                        if (isCheckedIn) "CHECKED IN" else "CHECKED OUT",
                        style = MaterialTheme.typography.labelLarge,
                        color = ringColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(state.employeeName.ifBlank { "Employee" }, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            if (isCheckedIn && state.checkInTime != null) {
                Text("Since: ${state.checkInTime}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            state.actionMsg?.let { msg ->
                Text(text = msg, color = if (msg.contains("Failed")) StatusRed else StatusGreen, style = MaterialTheme.typography.bodyMedium)
            }

            GoldButton(
                text = if (isCheckedIn) "Check Out" else "Check In",
                onClick = {
                    if (isCheckedIn) viewModel.checkOut(context, 0.0, 0.0)
                    else viewModel.checkIn(context, 0.0, 0.0)
                },
                icon = if (isCheckedIn) Icons.Default.ExitToApp else Icons.Default.Login,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────── EXPENSES ────────────────────────────────────────────────

@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load(context) }

    AppScaffold(title = "Expenses", onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))) {
            if (state.isLoading) CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            else if (state.expenses.isEmpty()) EmptyState("No expenses recorded", Icons.Default.AccountBalance)
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.expenses, key = { it.id }) { expense ->
                    GlassCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(expense.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text((expense.productId?.getOrNull(1) as? String) ?: "-", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(expense.date ?: "-", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SAR %.2f".format(expense.unitAmount * expense.quantity), style = MaterialTheme.typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                StatusBadge(expense.state)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────── DAY CLOSING ─────────────────────────────────────────────

@Composable
fun ClosingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    var performance by remember { mutableStateOf<com.tts.fieldsales.data.model.PerformanceData?>(null) }
    var route by remember { mutableStateOf<com.tts.fieldsales.data.model.DailyRoute?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repo.getPerformanceData().onSuccess { performance = it }
        repo.getTodayRoute().onSuccess { route = it }
        isLoading = false
    }

    AppScaffold(title = "Day Closing", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest)))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                GoldenCard {
                    Text("Today's Summary", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    performance?.let {
                        InfoRow("Sales Revenue", "SAR ${it.formattedSales}", Icons.Default.TrendingUp)
                        InfoRow("Daily Goal", "SAR ${it.formattedGoal}", Icons.Default.EmojiEvents)
                        InfoRow("Goal Progress", "${it.goalProgress}%", Icons.Default.DonutLarge)
                    }
                    route?.let {
                        GoldDivider(Modifier.padding(vertical = 8.dp))
                        InfoRow("Visits Completed", "${it.completedVisits}/${it.totalVisits}", Icons.Default.Place)
                        InfoRow("Route Distance", "${it.totalDistance} km", Icons.Default.Map)
                        InfoRow("Route Status", it.state?.replace("_", " ") ?: "-", Icons.Default.Route)
                    }
                }

                GoldButton(
                    text = "Print Daily Report",
                    onClick = { /* Show print sheet */ },
                    icon = Icons.Default.Print,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

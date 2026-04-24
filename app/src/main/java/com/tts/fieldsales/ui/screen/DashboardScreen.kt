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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tts.fieldsales.ui.components.*
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.DashboardViewModel
import kotlin.math.min

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "dash_anim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrownDark, BrownDarkest, BrownDeep)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── HEADER ─────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(BrownMedium, BrownDark)))
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Good ${getGreeting()}! 👋", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                state.userName.ifBlank { "Sales Rep" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                getCurrentDate(),
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Attendance badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (state.isCheckedIn) StatusGreen.copy(0.2f) else StatusAmber.copy(0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable { onNavigate("attendance") },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(8.dp).background(
                                        if (state.isCheckedIn) StatusGreen else StatusAmber, CircleShape
                                    ).then(if (state.isCheckedIn) Modifier.shadow(4.dp, CircleShape, ambientColor = StatusGreen, spotColor = StatusGreen) else Modifier))
                                    Text(
                                        if (state.isCheckedIn) "Checked In" else "Check In",
                                        color = if (state.isCheckedIn) StatusGreen else StatusAmber,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(onClick = { onNavigate("settings") }) {
                                Icon(Icons.Default.Settings, null, tint = GoldPrimary)
                            }
                        }
                    }
            }

            // ─── PERFORMANCE CARD ────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PerformanceCard(
                        todaySales = state.todaySales,
                        dailyGoal = state.dailyGoal,
                        progress = state.goalProgress,
                        glowAlpha = glowAlpha
                    )
                }
            }

            // ─── CHART ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GlassCard {
                        Text("7-Day Sales", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (state.chartData.isNotEmpty()) {
                            SalesBarChart(data = state.chartData)
                        } else {
                            ShimmerBox(Modifier.fillMaxWidth().height(60.dp))
                        }
                    }
                }
            }

            // ─── QUICK STATS ─────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickStatCard("Today Orders", state.todayOrders.toString(), Icons.Default.Receipt, GoldPrimary, Modifier.weight(1f))
                    QuickStatCard("Visits Done", "${state.visitsCompleted}/${state.totalVisits}", Icons.Default.Place, StatusGreen, Modifier.weight(1f))
                    QuickStatCard("Pending", state.pendingApprovals.toString(), Icons.Default.PendingActions, StatusAmber, Modifier.weight(1f))
                }
            }

            // ─── MODULE GRID ─────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                Padding(16.dp) {
                    Text("Modules", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Grid of modules (3 columns)
            val modules = getDashboardModules()
            items(modules.chunked(3)) { rowModules ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (module in rowModules) {
                        DashboardModuleCard(
                            module = module,
                            onClick = { onNavigate(module.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowModules.size < 3) {
                        for (i in 0 until (3 - rowModules.size)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ─── TAKE A BREAK BUTTON ──────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = BrownDeep, spotColor = BrownDeep),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(BrownDarkest, BrownMedium))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(BrownLight.copy(0.4f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalCafe, contentDescription = "Take a Break", tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                }
                                Text("TAKE A BREAK", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceCard(todaySales: Double, dailyGoal: Double, progress: Float, glowAlpha: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = GoldGlow.copy(glowAlpha), spotColor = GoldGlow.copy(glowAlpha))
            .background(Brush.linearGradient(listOf(BrownMedium, BrownCard)), RoundedCornerShape(20.dp))
            .border(1.dp, Brush.linearGradient(listOf(GoldPrimary.copy(0.8f), GoldDim.copy(0.3f), GoldPrimary.copy(0.6f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("Today's Revenue", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "SAR %.2f".format(todaySales),
                    style = MaterialTheme.typography.displaySmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("of SAR %.0f goal".format(dailyGoal), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            // Ring chart
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)
                    // Background arc
                    drawArc(GoldDim.copy(0.2f), 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
                    // Progress arc
                    drawArc(
                        brush = Brush.sweepGradient(listOf(GoldBright, GoldPrimary, GoldDim)),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // Linear progress bar
        Box(
            Modifier.fillMaxWidth().height(6.dp)
                .background(GoldDim.copy(0.2f), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(Brush.horizontalGradient(listOf(GoldBright, GoldPrimary)), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SalesBarChart(data: List<com.tts.fieldsales.data.model.DashboardStats>) {
    val maxVal = data.maxOfOrNull { it.total } ?: 1.0
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val heightFraction = if (maxVal > 0) (item.total / maxVal).toFloat() else 0f
            val animHeight by animateFloatAsState(
                targetValue = heightFraction,
                animationSpec = tween(800, easing = EaseOutBounce),
                label = "bar_height"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(animHeight.coerceAtLeast(0.05f))
                        .background(
                            Brush.verticalGradient(listOf(GoldBright, GoldPrimary)),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(item.day, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

data class DashboardModule(val title: String, val icon: ImageVector, val route: String, val color: Color, val badge: Int = 0)

private fun getDashboardModules() = listOf(
    DashboardModule("Routes", Icons.Default.Map, "routes", Color(0xFF673AB7)),
    DashboardModule("Customers", Icons.Default.People, "customers", Color(0xFF26A69A)),
    DashboardModule("Orders", Icons.Default.ShoppingCart, "orders", StatusBlue),
    DashboardModule("Invoices", Icons.Default.Receipt, "invoices", StatusRed),
    DashboardModule("Payments", Icons.Default.Payment, "payments", StatusAmber),
    DashboardModule("Returns", Icons.Default.AssignmentReturn, "returns", StatusRed),
    DashboardModule("Request", Icons.Default.LocalShipping, "request", Color(0xFF9C27B0)),
    DashboardModule("Stock", Icons.Default.Inventory, "stock", StatusGreen),
    DashboardModule("Products", Icons.Default.ShoppingBag, "products", Color(0xFFE91E63)),
    DashboardModule("Statement", Icons.Default.Description, "statement", StatusBlue),
    DashboardModule("Closing", Icons.Default.PowerSettingsNew, "closing", StatusBlue),
    DashboardModule("Expenses", Icons.Default.Calculate, "expenses", GoldPrimary),
)

@Composable
private fun DashboardModuleCard(module: DashboardModule, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "scale")

    Column(
        modifier = modifier
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = module.color.copy(0.2f), spotColor = module.color.copy(0.2f))
            .background(
                Brush.verticalGradient(listOf(BrownCardElevated, BrownCard)),
                RoundedCornerShape(18.dp)
            )
            .border(1.dp, module.color.copy(0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp)
                .background(module.color.copy(0.15f), RoundedCornerShape(14.dp))
                .border(1.dp, module.color.copy(0.3f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(module.icon, contentDescription = null, tint = module.color, modifier = Modifier.size(28.dp))
        }
        Text(
            module.title,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun Padding(all: Dp, content: @Composable () -> Unit) {
    Box(Modifier.padding(all)) { content() }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        else -> "Evening"
    }
}

private fun getCurrentDate(): String {
    val sdf = java.text.SimpleDateFormat("EEEE, d MMM yyyy", java.util.Locale.ENGLISH)
    return sdf.format(java.util.Date())
}

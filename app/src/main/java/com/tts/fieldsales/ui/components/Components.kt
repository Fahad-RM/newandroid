package com.tts.fieldsales.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.tts.fieldsales.ui.theme.*

// ─── GLASS CARD ─────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderGold: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val cardModifier = modifier
        .background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4A2C18), Color(0xFF2C1A0E)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
        .then(
            if (borderGold) Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(GoldPrimary.copy(0.6f), GoldDim.copy(0.2f), GoldPrimary.copy(0.4f))),
                shape = shape
            ) else Modifier
        )
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = cardModifier.padding(16.dp), content = content)
}

// ─── GOLDEN CARD (elevated) ────────────────────────────────────────────────────

@Composable
fun GoldenCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = GoldGlow, spotColor = GoldGlow)
            .background(
                brush = Brush.verticalGradient(listOf(BrownCardElevated, BrownCard)),
                shape = shape
            )
            .border(1.dp, Brush.linearGradient(listOf(GoldPrimary, GoldDim, GoldPrimary)), shape)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

// ─── STATUS BADGE ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(state: String, modifier: Modifier = Modifier) {
    val (label, bg, textColor) = when (state.lowercase()) {
        "draft" -> Triple("Draft", BrownLight.copy(0.3f), TextSecondary)
        "waiting_approval", "waiting" -> Triple("Pending", StatusAmber.copy(0.2f), StatusAmber)
        "sale", "done", "posted", "approved" -> Triple(
            if (state == "sale") "Confirmed" else if (state == "posted") "Posted" else "Approved",
            StatusGreen.copy(0.2f), StatusGreen
        )
        "cancelled", "cancel", "rejected" -> Triple("Cancelled", StatusRed.copy(0.2f), StatusRed)
        "in_progress" -> Triple("In Progress", StatusBlue.copy(0.2f), StatusBlue)
        "completed", "visited" -> Triple("Completed", StatusGreen.copy(0.2f), StatusGreen)
        "skipped" -> Triple("Skipped", StatusAmber.copy(0.15f), StatusAmber)
        "new" -> Triple("New", GoldPrimary.copy(0.2f), GoldPrimary)
        else -> Triple(state.replace("_", " ").replaceFirstChar { it.uppercase() }, BrownLight.copy(0.3f), TextSecondary)
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

// ─── GOLD BUTTON ──────────────────────────────────────────────────────────────

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = TextOnGold
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(GoldBright.copy(if (enabled) alpha else 0.4f), GoldPrimary.copy(if (enabled) 1f else 0.4f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = TextOnGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextOnGold)
                }
            }
        }
    }
}

// ─── OUTLINE GOLD BUTTON ──────────────────────────────────────────────────────

@Composable
fun OutlineGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (enabled) GoldPrimary else GoldDim),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── SECTION HEADER ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        action?.invoke()
    }
}

// ─── GOLD DIVIDER ─────────────────────────────────────────────────────────────

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldDim.copy(0.5f), Color.Transparent)))
    )
}

// ─── LOADING SHIMMER ──────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "shimmer_translate"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BrownCard, BrownCardElevated, BrownCard),
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                )
            )
    )
}

// ─── INFO ROW ────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector? = null, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = GoldDim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ─── AMOUNT DISPLAY ──────────────────────────────────────────────────────────

@Composable
fun AmountDisplay(label: String, amount: Double, currency: String = "SAR", isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (isTotal) TextPrimary else TextSecondary,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "%.2f %s".format(amount, currency),
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isTotal) GoldPrimary else TextPrimary,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ─── EMPTY STATE ─────────────────────────────────────────────────────────────

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Default.Inbox, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GoldDim.copy(0.5f), modifier = Modifier.size(64.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.invoke()
    }
}

// ─── SEARCH BAR ──────────────────────────────────────────────────────────────

@Composable
fun GoldSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldDim) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = GoldDim.copy(0.4f),
            cursorColor = GoldPrimary,
            focusedContainerColor = BrownCard,
            unfocusedContainerColor = BrownCard
        )
    )
}

// ─── GOLD TEXT FIELD ──────────────────────────────────────────────────────────

@Composable
fun GoldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMsg: String? = null,
    singleLine: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    readOnly: Boolean = false,
    maxLines: Int = 1
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, color = if (isError) StatusRed else TextSecondary) },
            leadingIcon = if (leadingIcon != null) ({ Icon(leadingIcon, null, tint = if (isError) StatusRed else GoldDim) }) else null,
            trailingIcon = trailingContent,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            readOnly = readOnly,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = if (isError) StatusRed else GoldPrimary,
                unfocusedBorderColor = if (isError) StatusRed else GoldDim.copy(0.4f),
                cursorColor = GoldPrimary,
                focusedContainerColor = BrownCard,
                unfocusedContainerColor = BrownCard,
                focusedLabelColor = if (isError) StatusRed else GoldPrimary,
                unfocusedLabelColor = TextMuted
            )
        )
        if (isError && errorMsg != null) {
            Text(errorMsg, color = StatusRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        }
    }
}

// ─── SCREEN SCAFFOLD ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = BrownDarkest,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = GoldPrimary)
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrownDark,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = GoldPrimary
                )
            )
        },
        floatingActionButton = floatingActionButton,
        content = content
    )
}

// ─── QUICK STAT CARD ─────────────────────────────────────────────────────────

@Composable
fun QuickStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(color.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

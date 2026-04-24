package com.tts.fieldsales.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tts.fieldsales.ui.theme.*
import com.tts.fieldsales.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Launch effect to handle success
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_offset"
    )

    var logoVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200); logoVisible = true
        delay(500); formVisible = true
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.radialGradient(
                colors = listOf(BrownSurface.copy(bgOffset * 0.5f + 0.3f), BrownDarkest),
                center = Offset(bgOffset * 400f, 300f),
                radius = 800f
            ))
    ) {
        // Decorative gold circles
        Box(
            Modifier.size(200.dp)
                .offset((-60).dp, (-60).dp)
                .background(GoldPrimary.copy(0.05f), CircleShape)
        )
        Box(
            Modifier.size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .background(GoldPrimary.copy(0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Logo & Title
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(700)) + slideInVertically { -60 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App Icon
                    Box(
                        modifier = Modifier.size(90.dp)
                            .shadow(24.dp, CircleShape, ambientColor = GoldGlow, spotColor = GoldGlow)
                            .background(Brush.radialGradient(listOf(GoldBright, GoldPrimary, GoldDark)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Logo",
                            tint = TextOnGold,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "TTS Field Sales",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Professional Sales Management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Login Form Card
            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(700)) + slideInVertically { 80 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = GoldGlow.copy(0.3f))
                        .background(
                            Brush.verticalGradient(listOf(BrownCardElevated, BrownCard)),
                            RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, Brush.linearGradient(listOf(GoldPrimary.copy(0.5f), GoldDim.copy(0.2f))), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Sign In", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Connect to your Odoo server", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                    GoldDivider(Modifier.padding(vertical = 4.dp))

                    // Server URL
                    LoginField(
                        value = state.odooUrl,
                        onValueChange = viewModel::setUrl,
                        label = "Odoo Server URL",
                        icon = Icons.Default.Language,
                        placeholder = "https://your-company.odoo.com"
                    )

                    // Database
                    LoginField(
                        value = state.database,
                        onValueChange = viewModel::setDatabase,
                        label = "Database Name",
                        icon = Icons.Default.Storage,
                        placeholder = "your_database"
                    )

                    // Username
                    LoginField(
                        value = state.username,
                        onValueChange = viewModel::setUsername,
                        label = "Username / Email",
                        icon = Icons.Default.Person,
                        keyboardType = KeyboardType.Email
                    )

                    // Password
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = GoldDim) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = GoldDim)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = loginFieldColors()
                    )

                    // Error message
                    AnimatedVisibility(visible = state.errorMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(StatusRed.copy(0.15f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            Text(state.errorMessage ?: "", color = StatusRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Login Button
                    Button(
                        onClick = { viewModel.login(context) },
                        enabled = !state.isLoading && state.odooUrl.isNotBlank() && state.username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        if (!state.isLoading) listOf(GoldBright, GoldPrimary)
                                        else listOf(GoldDim, GoldDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = TextOnGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Connecting...", color = TextOnGold, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Login, null, tint = TextOnGold, modifier = Modifier.size(20.dp))
                                    Text("Sign In", style = MaterialTheme.typography.titleMedium, color = TextOnGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Footer
            Text(
                "TTS Field Sales v1.0 • Powered by Odoo",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextMuted, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(icon, null, tint = GoldDim) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = loginFieldColors()
    )
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = GoldDim.copy(0.4f),
    cursorColor = GoldPrimary,
    focusedContainerColor = BrownDark,
    unfocusedContainerColor = BrownDark,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextMuted
)

private fun GoldDivider(modifier: Modifier = Modifier): @Composable () -> Unit = {}

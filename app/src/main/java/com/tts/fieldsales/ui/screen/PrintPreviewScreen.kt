package com.tts.fieldsales.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────── PRINT PREVIEW ───────────────────────────────────────────

enum class PreviewState { LOADING, SUCCESS, ERROR }

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    reportName: String,
    recordId: Int,
    recordName: String = "Document",
    onBack: () -> Unit,
    onSendToPrinter: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    val scope = rememberCoroutineScope()

    var htmlContent by remember { mutableStateOf<String?>(null) }
    var previewState by remember { mutableStateOf(PreviewState.LOADING) }
    var errorMsg by remember { mutableStateOf("") }
    var pageLoadProgress by remember { mutableStateOf(0) }
    var zoomLevel by remember { mutableStateOf(100) }

    // WebView instance kept stable
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                textZoom = 100
                allowFileAccess = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
        }
    }

    // Fetch the HTML from Odoo
    LaunchedEffect(reportName, recordId) {
        previewState = PreviewState.LOADING
        withContext(Dispatchers.IO) {
            repo.getReportHtml(reportName, recordId).fold(
                onSuccess = { html ->
                    htmlContent = injectThermalPreviewCss(html, prefs.getPaperWidth())
                    previewState = PreviewState.SUCCESS
                },
                onFailure = { e ->
                    // Fallback: generate a local preview template
                    htmlContent = generateFallbackHtml(reportName, recordId, recordName, e.message)
                    previewState = PreviewState.SUCCESS
                    errorMsg = "Showing local preview: ${e.message?.take(60)}"
                }
            )
        }
    }

    // Load HTML into WebView when content is ready
    LaunchedEffect(htmlContent) {
        htmlContent?.let { html ->
            val baseUrl = prefs.getOdooUrl().trimEnd('/')
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    pageLoadProgress = 10
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    pageLoadProgress = 100
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
            }
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    pageLoadProgress = newProgress
                }
            }
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        containerColor = BrownDarkest,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Print Preview", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(recordName, color = GoldDim, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = GoldPrimary)
                        }
                    },
                    actions = {
                        // Zoom out
                        IconButton(onClick = {
                            zoomLevel = (zoomLevel - 25).coerceAtLeast(50)
                            webView.settings.textZoom = zoomLevel
                        }) { Icon(Icons.Default.ZoomOut, "Zoom Out", tint = TextSecondary) }
                        // Zoom in
                        IconButton(onClick = {
                            zoomLevel = (zoomLevel + 25).coerceAtMost(200)
                            webView.settings.textZoom = zoomLevel
                        }) { Icon(Icons.Default.ZoomIn, "Zoom In", tint = TextSecondary) }
                        // Reload
                        IconButton(onClick = {
                            scope.launch {
                                previewState = PreviewState.LOADING
                                repo.getReportHtml(reportName, recordId).onSuccess { html ->
                                    htmlContent = injectThermalPreviewCss(html, prefs.getPaperWidth())
                                    previewState = PreviewState.SUCCESS
                                }
                            }
                        }) { Icon(Icons.Default.Refresh, "Reload", tint = TextSecondary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrownDark,
                        titleContentColor = TextPrimary
                    )
                )
                // Progress bar
                AnimatedVisibility(visible = pageLoadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageLoadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = GoldPrimary,
                        trackColor = GoldDim.copy(0.2f)
                    )
                }
                if (errorMsg.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(StatusAmber.copy(0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = StatusAmber, modifier = Modifier.size(14.dp))
                        Text(errorMsg, color = StatusAmber, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = BrownDark, tonalElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Paper size indicator
                    Box(
                        Modifier.border(1.dp, GoldDim.copy(0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (prefs.getPaperWidth() == 80) "4\" (80mm)" else "3\" (58mm)",
                            color = GoldDim, style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Share HTML button
                    OutlinedButton(
                        onClick = { shareHtml(context, htmlContent ?: "", recordName) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                        border = BorderStroke(1.dp, GoldPrimary.copy(0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = previewState == PreviewState.SUCCESS
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }

                    // Print button
                    Button(
                        onClick = {
                            if (onSendToPrinter != null) {
                                onSendToPrinter()
                            } else {
                                // Use Android system print
                                triggerSystemPrint(context, webView, recordName)
                            }
                        },
                        enabled = previewState == PreviewState.SUCCESS,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Print", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF111111))))
        ) {
            when (previewState) {
                PreviewState.LOADING -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                        Text("Loading template from Odoo...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                PreviewState.SUCCESS, PreviewState.ERROR -> {
                    // Paper shadow container
                    Box(
                        Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AndroidView(
                            factory = { webView },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Zoom level badge
            AnimatedVisibility(
                visible = zoomLevel != 100,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Surface(
                    color = BrownDark.copy(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        "${zoomLevel}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = GoldPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    // Cleanup WebView on dispose
    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }
}

// ─────────────────── CSS INJECTION ───────────────────────────────────────────

private fun injectThermalPreviewCss(html: String, paperWidthMm: Int): String {
    val widthPx = if (paperWidthMm == 80) "302px" else "220px"
    val css = """
        <style>
        * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
        body {
            background: #f0f0f0 !important;
            display: flex;
            justify-content: center;
            padding: 20px;
            margin: 0;
            font-family: 'Courier New', Courier, monospace;
        }
        .thermal-wrapper {
            background: white;
            width: $widthPx;
            padding: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            border-radius: 4px;
        }
        /* Ensure all content fits thermal width */
        table { width: 100% !important; font-size: 10px !important; }
        th, td { font-size: 10px !important; padding: 2px 4px !important; }
        h1, h2, h3 { font-size: 13px !important; text-align: center !important; }
        p { font-size: 10px !important; margin: 2px 0 !important; }
        img { max-width: 100px !important; display: block; margin: auto; }
        hr { border: 1px dashed #999; }
        @media screen {
            body { background: #2a1a0a; }
        }
        </style>
    """.trimIndent()

    // Try to wrap content in thermal-wrapper div
    return if (html.contains("</head>")) {
        val withCss = html.replace("</head>", "$css</head>")
        if (withCss.contains("<body")) {
            withCss.replace(Regex("(<body[^>]*>)"), "$1<div class='thermal-wrapper'>")
                   .replace("</body>", "</div></body>")
        } else withCss
    } else {
        // Raw HTML without head — wrap entirely
        "<html><head>$css</head><body><div class='thermal-wrapper'>$html</div></body></html>"
    }
}

// ─────────────────── FALLBACK HTML ───────────────────────────────────────────

private fun generateFallbackHtml(reportName: String, recordId: Int, recordName: String, error: String?): String {
    val reportLabel = reportName.replace(".", " ").replace("_", " ")
        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8"/>
        <style>
          body { font-family: 'Courier New', monospace; background: #f0f0f0; padding: 20px; display: flex; justify-content: center; }
          .receipt { background: white; width: 280px; padding: 15px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
          .center { text-align: center; }
          .bold { font-weight: bold; }
          .line { border-top: 1px dashed #999; margin: 8px 0; }
          .small { font-size: 10px; }
          .logo { font-size: 18px; font-weight: bold; letter-spacing: 2px; }
          .info { font-size: 11px; margin: 4px 0; }
          .warn { background: #fff3cd; padding: 10px; border-radius: 4px; font-size: 10px; color: #856404; margin-top: 10px; }
        </style>
        </head>
        <body>
        <div class="receipt">
          <div class="center logo">TTS FIELD SALES</div>
          <div class="center small">Preview Mode — Odoo Not Reachable</div>
          <div class="line"></div>
          <div class="info bold">$reportLabel</div>
          <div class="info">Record: $recordName (#$recordId)</div>
          <div class="line"></div>
          <div class="info">This is a placeholder preview.</div>
          <div class="info">The actual template will be fetched</div>
          <div class="info">from Odoo when connected.</div>
          <div class="line"></div>
          <div class="warn">
            ⚠ Could not load from server:<br/>
            ${error?.take(100) ?: "Server unreachable"}
          </div>
        </div>
        </body>
        </html>
    """.trimIndent()
}

// ─────────────────── SYSTEM PRINT / SHARE ────────────────────────────────────

private fun triggerSystemPrint(context: android.content.Context, webView: WebView, jobName: String) {
    try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE)
                as android.print.PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        val printAttributes = android.print.PrintAttributes.Builder()
            .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
            .setResolution(android.print.PrintAttributes.Resolution("pdf_resolution", "PDF Resolution", 600, 600))
            .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, printAdapter, printAttributes)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Print failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun shareHtml(context: android.content.Context, html: String, name: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(android.content.Intent.EXTRA_SUBJECT, name)
            putExtra(android.content.Intent.EXTRA_TEXT, html)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share $name"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Share failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

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
import com.tts.fieldsales.print.ThermalPrintManager
import com.tts.fieldsales.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────── PRINT PREVIEW ───────────────────────────────────────────

enum class PreviewStatus { LOADING, SUCCESS, ERROR }

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    reportName: String,
    recordId: Int,
    recordName: String = "Document",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { OdooRepository(prefs) }
    val scope = rememberCoroutineScope()

    var htmlContent by remember { mutableStateOf<String?>(null) }
    var previewStatus by remember { mutableStateOf(PreviewStatus.LOADING) }
    var errorMsg by remember { mutableStateOf("") }
    var pageLoadProgress by remember { mutableStateOf(0) }
    var zoomLevel by remember { mutableStateOf(100) }
    var paperWidthMm by remember { mutableStateOf(58) }

    LaunchedEffect(Unit) { paperWidthMm = prefs.getPaperWidth() }

    // Fetch the HTML from Odoo
    LaunchedEffect(reportName, recordId) {
        previewStatus = PreviewStatus.LOADING
        withContext(Dispatchers.IO) {
            repo.getReportHtml(reportName, recordId).fold(
                onSuccess = { html ->
                    htmlContent = injectOdooStyleCss(html, prefs.getPaperWidth())
                    previewStatus = PreviewStatus.SUCCESS
                },
                onFailure = { e ->
                    // Fallback: generate a local preview template
                    htmlContent = generateFallbackHtml(reportName, recordId, recordName, e.message)
                    previewStatus = PreviewStatus.SUCCESS
                    errorMsg = "Server unreachable. Showing draft layout."
                }
            )
        }
    }

    Scaffold(
        containerColor = BrownDarkest,
        topBar = {
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
                    IconButton(onClick = {
                        zoomLevel = (zoomLevel + 10).coerceAtMost(200)
                    }) { Icon(Icons.Default.ZoomIn, "Zoom In", tint = GoldPrimary) }
                    
                    IconButton(onClick = {
                        zoomLevel = (zoomLevel - 10).coerceAtLeast(50)
                    }) { Icon(Icons.Default.ZoomOut, "Zoom Out", tint = GoldPrimary) }

                    IconButton(onClick = {
                        scope.launch {
                            previewStatus = PreviewStatus.LOADING
                            repo.getReportHtml(reportName, recordId).onSuccess { html ->
                                htmlContent = injectOdooStyleCss(html, prefs.getPaperWidth())
                                previewStatus = PreviewStatus.SUCCESS
                                errorMsg = ""
                            }
                        }
                    }) { Icon(Icons.Default.Refresh, "Reload", tint = TextSecondary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownDark)
            )
        },
        bottomBar = {
            Surface(color = BrownDark, tonalElevation = 4.dp) {
                Column {
                    if (pageLoadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageLoadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = GoldPrimary,
                            trackColor = GoldDim.copy(0.2f)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BrownMedium,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldDim.copy(0.3f))
                        ) {
                            Text(
                                if (paperWidthMm == 80) "4\" (80mm)" else "3\" (58mm)",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = GoldDim, style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = { shareHtml(context, htmlContent ?: "", recordName) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrownMedium, contentColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share")
                        }

                        Button(
                            onClick = {
                                htmlContent?.let { html ->
                                    ThermalPrintManager.printHtmlToBluetooth(context, html, paperWidthMm, recordName)
                                }
                            },
                            enabled = previewStatus == PreviewStatus.SUCCESS,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Print", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF111111))))) {
            if (previewStatus == PreviewStatus.LOADING) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.align(Alignment.Center))
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    pageLoadProgress = 10
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageLoadProgress = 100
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageLoadProgress = newProgress
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        htmlContent?.let { html ->
                            scope.launch {
                                val baseUrl = prefs.getOdooUrl().trimEnd('/')
                                view.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                                view.setInitialScale(zoomLevel)
                            }
                        }
                    }
                )
                
                if (errorMsg.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                        color = StatusAmber.copy(0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMsg, color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun injectOdooStyleCss(html: String, paperWidthMm: Int): String {
    val widthPx = if (paperWidthMm == 80) "302px" else "220px"
    val css = """
        <style>
        @page { margin: 0; size: ${paperWidthMm}mm auto; }
        body { 
            background: #fff !important; 
            color: #000 !important;
            margin: 0 !important;
            padding: 5px !important;
            width: $widthPx !important;
            font-family: 'Arial', sans-serif !important;
            font-size: 12px !important;
        }
        .container, .container-fluid { padding: 0 !important; width: 100% !important; max-width: 100% !important; }
        table { width: 100% !important; border-collapse: collapse !important; margin-bottom: 10px !important; }
        th, td { padding: 2px 4px !important; border-bottom: 1px solid #eee !important; font-size: 11px !important; }
        .row { display: flex !important; flex-wrap: wrap !important; margin: 0 !important; }
        .col-6 { width: 50% !important; }
        .col-4 { width: 33.33% !important; }
        .col-8 { width: 66.66% !important; }
        .text-right { text-align: right !important; }
        .text-center { text-align: center !important; }
        img { max-width: 100% !important; height: auto !important; }
        h1, h2, h3, h4 { margin: 5px 0 !important; text-align: center !important; font-size: 14px !important; }
        hr { border: 0 !important; border-top: 1px dashed #000 !important; margin: 5px 0 !important; }
        /* Fix for Odoo thermal templates */
        .o_report_layout_standard { padding: 0 !important; }
        </style>
    """.trimIndent()
    
    return if (html.contains("<head>")) {
        html.replace("<head>", "<head>$css")
    } else {
        "<html><head>$css</head><body>$html</body></html>"
    }
}

private fun generateFallbackHtml(reportName: String, recordId: Int, recordName: String, error: String?): String {
    return """
        <div style="text-align:center; padding: 20px;">
            <h2 style="color:#856404">Draft Layout</h2>
            <p><strong>$recordName</strong></p>
            <hr/>
            <div style="text-align:left; font-family:monospace">
                <p>Report: $reportName</p>
                <p>ID: $recordId</p>
                <p>Status: OFFLINE PREVIEW</p>
            </div>
            <hr/>
            <p style="font-size:10px; color:red">Error: ${error ?: "Unknown error"}</p>
        </div>
    """.trimIndent()
}

private fun shareHtml(context: android.content.Context, html: String, name: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(android.content.Intent.EXTRA_SUBJECT, name)
            putExtra(android.content.Intent.EXTRA_TEXT, html)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share $name"))
    } catch (e: Exception) {}
}

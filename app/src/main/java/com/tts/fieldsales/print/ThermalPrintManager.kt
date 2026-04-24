package com.tts.fieldsales.print

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ThermalPrintManager {

    // Report name constants matching Odoo module
    const val REPORT_SALE_ORDER = "tts_field_sales.action_report_thermal_order_v2"
    const val REPORT_INVOICE = "tts_field_sales.action_report_thermal_invoice"
    const val REPORT_PAYMENT = "tts_field_sales.action_report_thermal_payment"
    const val REPORT_RETURN = "tts_field_sales.action_report_thermal_return_receipt"
    const val REPORT_STATEMENT = "tts_field_sales.action_report_thermal_statement"
    const val REPORT_OUTSTANDING = "tts_field_sales.action_report_thermal_outstanding"
    const val REPORT_DAILY = "tts_field_sales.action_report_thermal_daily_report"
    const val REPORT_ROUTE = "tts_field_sales.action_report_route_execution"

    /**
     * Print an HTML string to a Bluetooth thermal printer via Android PrintManager.
     * Uses WebView to render HTML exactly as the Odoo web app does.
     *
     * @param context Android context
     * @param html HTML content from Odoo report
     * @param paperWidthMm 58 for 3-inch, 80 for 4-inch
     * @param jobName Name of the print job
     */
    fun printHtmlToBluetooth(
        context: Context,
        html: String,
        paperWidthMm: Int = 58,
        jobName: String = "TTS Field Sales"
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = false
                setSupportZoom(false)
            }

            // Inject CSS to match thermal printer paper width
            val widthPx = if (paperWidthMm == 80) "302px" else "220px"
            val styledHtml = """
                <html><head>
                <meta charset="UTF-8">
                <style>
                    @page { margin: 1mm; size: ${paperWidthMm}mm auto; }
                    body { width: ${widthPx}; margin: 0; padding: 0; font-family: sans-serif; font-size: 9px; color: #000; }
                    * { box-sizing: border-box; }
                    img { max-width: 100%; }
                    table { width: 100%; border-collapse: collapse; }
                </style>
                </head><body>
                $html
                </body></html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    createPrintJob(context, view, jobName, paperWidthMm)
                }
            }
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        }
    }

    private fun createPrintJob(context: Context, webView: WebView, jobName: String, paperWidthMm: Int): PrintJob? {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val paperWidth = if (paperWidthMm == 80) {
            // 80mm = ~3.15 inches = 3150 mils
            PrintAttributes.MediaSize("thermal_80mm", "Thermal 80mm", 3150, 8270)
        } else {
            // 58mm = ~2.28 inches = 2280 mils
            PrintAttributes.MediaSize("thermal_58mm", "Thermal 58mm", 2280, 8270)
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(paperWidth)
            .setResolution(PrintAttributes.Resolution("default", "Default", 203, 203))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        return printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Build a full HTML page wrapper for thermal printing with the correct styles
     */
    fun wrapHtmlForPrint(html: String, paperWidthMm: Int): String {
        val widthPx = if (paperWidthMm == 80) "302px" else "220px"
        return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  @page { margin: 2mm 1mm; size: ${paperWidthMm}mm auto; }
  html, body { width: ${widthPx}; margin: 0; padding: 2px; background: white; color: black; }
  body { font-family: Arial, Helvetica, sans-serif; font-size: 8px; line-height: 1.15; }
  * { box-sizing: border-box; max-width: 100%; }
  img { max-width: 100%; height: auto; }
  table { width: 100%; border-collapse: collapse; font-size: 8px; }
  td, th { padding: 1px 2px; }
  .page { width: 100%; overflow: hidden; }
  /* Odoo Bootstrap overrides */
  .row { display: flex; flex-wrap: wrap; margin: 0; }
  .col-4, .col-6, .col-8 { padding: 0; }
  .col-4 { width: 33.33%; }
  .col-6 { width: 50%; }
  .col-8 { width: 66.66%; }
  .text-center { text-align: center; }
  .text-right { text-align: right; }
  .p-0 { padding: 0; }
</style>
</head>
<body>
$html
</body>
</html>"""
    }
}

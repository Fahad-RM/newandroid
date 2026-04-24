package com.tts.fieldsales.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")

    // Orders
    object Orders : Screen("orders")
    object NewOrder : Screen("new_order")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(id: Int) = "order_detail/$id"
    }

    // Invoices
    object Invoices : Screen("invoices")
    object InvoiceDetail : Screen("invoice_detail/{invoiceId}") {
        fun createRoute(id: Int) = "invoice_detail/$id"
    }

    // Payments
    object Payments : Screen("payments")
    object NewPayment : Screen("new_payment")

    // Returns
    object Returns : Screen("returns")
    object NewReturn : Screen("new_return")

    // Customers
    object Customers : Screen("customers")
    object CustomerDetail : Screen("customer_detail/{customerId}") {
        fun createRoute(id: Int) = "customer_detail/$id"
    }
    object NewCustomer : Screen("new_customer")

    // Visits & Routes
    object Visits : Screen("visits")
    object Routes : Screen("routes")
    object VisitDetail : Screen("visit_detail/{visitId}") {
        fun createRoute(id: Int) = "visit_detail/$id"
    }

    // Expenses
    object Expenses : Screen("expenses")
    object NewExpense : Screen("new_expense")

    // Attendance & Closing
    object Attendance : Screen("attendance")
    object Closing : Screen("closing")

    // Printer Settings
    object PrinterSetup : Screen("printer_setup")

    // Print Preview
    object PrintPreview : Screen("print_preview/{reportName}/{recordId}/{recordName}") {
        fun createRoute(reportName: String, recordId: Int, recordName: String): String {
            val encoded = java.net.URLEncoder.encode(reportName, "UTF-8")
            val nameEncoded = java.net.URLEncoder.encode(recordName, "UTF-8")
            return "print_preview/$encoded/$recordId/$nameEncoded"
        }
    }
}

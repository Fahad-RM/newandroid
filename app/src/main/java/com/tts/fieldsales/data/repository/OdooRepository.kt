package com.tts.fieldsales.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.tts.fieldsales.data.api.OdooClient
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.data.prefs.AppPreferences

class OdooRepository(private val prefs: AppPreferences) {

    private val gson = Gson()

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun buildRpc(method: String, model: String, args: List<Any> = emptyList(), kwargs: Map<String, Any> = emptyMap()) =
        OdooClient.buildJsonRpcBody(method, model, args, kwargs)

    private suspend fun <T> callModel(
        method: String, model: String,
        args: List<Any> = emptyList(),
        kwargs: Map<String, Any> = emptyMap(),
        clazz: Class<T>
    ): Result<T> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/$model/$method"
        val body = buildRpc(method, model, args, kwargs)
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result
            ?: throw Exception(response.body()?.error?.data?.message ?: "Empty response")
        gson.fromJson(result, clazz)
    }

    private suspend fun callModelRaw(
        method: String, model: String,
        args: List<Any> = emptyList(),
        kwargs: Map<String, Any> = emptyMap()
    ): Result<JsonElement> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/$model/$method"
        val body = buildRpc(method, model, args, kwargs)
        val response = OdooClient.getService().call(url, body)
        response.body()?.result ?: throw Exception(response.body()?.error?.data?.message ?: "Empty response")
    }

    // ─── AUTH ────────────────────────────────────────────────────────────────────

    suspend fun login(url: String, db: String, username: String, password: String): Result<Int> = runCatching {
        OdooClient.initialize(url)
        val authUrl = url.trimEnd('/') + "/web/session/authenticate"
        val body = OdooClient.buildAuthBody(
            mapOf(
                "db" to db,
                "login" to username,
                "password" to password
            )
        )
        val response = OdooClient.getService().call(authUrl, body)
        val result = response.body()?.result ?: throw Exception("Login failed — no response from server")
        val obj = gson.fromJson(result, com.google.gson.JsonElement::class.java).asJsonObject
        val uid = obj.get("uid")?.let { if (!it.isJsonNull) it.asInt else null }
            ?: throw Exception("Invalid credentials. Please check your username and password.")
        val name = obj.get("name")?.asString ?: username
        prefs.saveLoginInfo(url, db, username, password, uid, name)
        uid
    }

    suspend fun getUserDetails(): Result<UserDetails> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.users/search_read"
        val body = buildRpc("search_read", "res.users", kwargs = mapOf(
            "domain" to listOf(listOf("id", "=", uid)),
            "fields" to listOf("name","field_sales_pin","image_128","field_sales_global_access",
                "field_sales_can_see_customers","field_sales_can_see_orders","field_sales_can_see_invoices",
                "field_sales_can_see_returns","field_sales_can_see_payments","field_sales_can_see_visits",
                "field_sales_can_see_expenses","field_sales_can_see_closing","field_sales_can_see_routes",
                "field_sales_allowed_warehouse_ids","field_sales_allowed_journal_ids","field_sales_daily_goal"),
            "limit" to 1
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Cannot get user")
        val arr = gson.fromJson(result, JsonArray::class.java)
        gson.fromJson(arr.first(), UserDetails::class.java)
    }

    // ─── DASHBOARD ───────────────────────────────────────────────────────────────

    suspend fun getPerformanceData(): Result<PerformanceData> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/get_performance_data"
        val body = buildRpc("get_performance_data", "sale.order", kwargs = mapOf("user_id" to uid))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("No performance data")
        gson.fromJson(result, PerformanceData::class.java)
    }

    // ─── CUSTOMERS ───────────────────────────────────────────────────────────────

    suspend fun getCustomers(searchTerm: String = ""): Result<List<Partner>> = runCatching {
        val uid = prefs.getUserId()
        val domain = mutableListOf<Any>(listOf("field_sales_user_ids", "in", listOf(uid)), listOf("is_company", "=", true))
        if (searchTerm.isNotBlank()) domain.add(listOf("name", "ilike", searchTerm))
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/search_read"
        val body = buildRpc("search_read", "res.partner", kwargs = mapOf(
            "domain" to domain,
            "fields" to listOf("name","ref","street","city","phone","mobile","email","vat","partner_latitude","partner_longitude","arabic_customer_name","approval_status","credit","credit_limit","image_128","total_overdue"),
            "order" to "name asc",
            "limit" to 200
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Partner>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun createCustomer(vals: Map<String, Any>): Result<Int> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/create_app_customer"
        val body = buildRpc("create_app_customer", "res.partner", kwargs = mapOf("vals" to vals))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Create customer failed")
        gson.fromJson(result, Int::class.java)
    }

    // ─── SALE ORDERS ─────────────────────────────────────────────────────────────

    suspend fun getSaleOrders(states: List<String> = listOf("draft","waiting_approval","sale","done")): Result<List<SaleOrder>> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/search_read"
        val body = buildRpc("search_read", "sale.order", kwargs = mapOf(
            "domain" to listOf(listOf("user_id", "=", uid), listOf("state", "in", states)),
            "fields" to listOf("name","state","date_order","partner_id","user_id","amount_untaxed","amount_tax","amount_total","invoice_ids","invoice_status","warehouse_id"),
            "order" to "date_order desc",
            "limit" to 100
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<SaleOrder>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun getOrderLines(orderId: Int): Result<List<SaleOrderLine>> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order.line/search_read"
        val body = buildRpc("search_read", "sale.order.line", kwargs = mapOf(
            "domain" to listOf(listOf("order_id", "=", orderId)),
            "fields" to listOf("product_id","name","product_uom_qty","product_uom","price_unit","discount","price_subtotal","price_total","tax_id","display_type"),
            "order" to "id asc"
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<SaleOrderLine>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun createSaleOrder(partnerId: Int, warehouseId: Int, lines: List<Map<String, Any>>): Result<Int> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/create"
        val lineCommands = lines.map { line -> listOf(0, 0, line) }
        val vals = mapOf(
            "partner_id" to partnerId,
            "user_id" to uid,
            "warehouse_id" to warehouseId,
            "order_line" to lineCommands
        )
        val body = buildRpc("create", "sale.order", args = listOf(listOf(vals)))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Order creation failed")
        gson.fromJson(result, Int::class.java)
    }

    suspend fun submitOrderForApproval(orderId: Int): Result<Boolean> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/action_request_approval"
        val body = buildRpc("action_request_approval", "sale.order", args = listOf(listOf(orderId)))
        OdooClient.getService().call(url, body)
        true
    }

    suspend fun confirmOrder(orderId: Int): Result<Boolean> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/action_confirm"
        val body = buildRpc("action_confirm", "sale.order", args = listOf(listOf(orderId)))
        OdooClient.getService().call(url, body)
        true
    }

    suspend fun createInvoiceFromOrder(orderId: Int): Result<List<Int>> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/sale.order/action_app_create_invoice"
        val body = buildRpc("action_app_create_invoice", "sale.order", args = listOf(listOf(orderId)))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        gson.fromJson(result, type)
    }

    // ─── INVOICES ────────────────────────────────────────────────────────────────

    suspend fun getInvoices(moveType: String = "out_invoice"): Result<List<Invoice>> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.move/search_read"
        val body = buildRpc("search_read", "account.move", kwargs = mapOf(
            "domain" to listOf(listOf("move_type", "=", moveType), listOf("invoice_user_id", "=", uid)),
            "fields" to listOf("name","move_type","state","partner_id","invoice_date","invoice_date_due","amount_untaxed","amount_tax","amount_total","amount_residual","invoice_user_id","ref","payment_state","field_sales_ref"),
            "order" to "invoice_date desc",
            "limit" to 100
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Invoice>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun getInvoiceLines(invoiceId: Int): Result<List<InvoiceLine>> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.move.line/search_read"
        val body = buildRpc("search_read", "account.move.line", kwargs = mapOf(
            "domain" to listOf(listOf("move_id", "=", invoiceId), listOf("display_type", "not in", listOf("line_section","line_note"))),
            "fields" to listOf("product_id","name","quantity","price_unit","price_subtotal","price_total","discount","product_uom_id","tax_ids","display_type")
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<InvoiceLine>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun postInvoice(invoiceId: Int): Result<Boolean> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.move/action_post"
        val body = buildRpc("action_post", "account.move", args = listOf(listOf(invoiceId)))
        OdooClient.getService().call(url, body)
        true
    }

    // ─── PAYMENTS ────────────────────────────────────────────────────────────────

    suspend fun getPayments(): Result<List<Payment>> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.payment/search_read"
        val body = buildRpc("search_read", "account.payment", kwargs = mapOf(
            "domain" to listOf(listOf("create_uid", "=", uid), listOf("payment_type", "=", "inbound")),
            "fields" to listOf("name","partner_id","amount","date","journal_id","state","field_sales_status","field_sales_ref","create_uid","currency_id"),
            "order" to "date desc",
            "limit" to 100
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Payment>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun createPayment(partnerId: Int, amount: Double, journalId: Int, memo: String = ""): Result<Int> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.payment/create"
        val vals = mapOf(
            "partner_id" to partnerId,
            "amount" to amount,
            "journal_id" to journalId,
            "payment_type" to "inbound",
            "partner_type" to "customer",
            "memo" to memo
        )
        val body = buildRpc("create", "account.payment", args = listOf(listOf(vals)))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Payment creation failed")
        gson.fromJson(result, Int::class.java)
    }

    suspend fun getJournals(): Result<List<Journal>> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.journal/search_read"
        val body = buildRpc("search_read", "account.journal", kwargs = mapOf(
            "domain" to listOf(listOf("type", "in", listOf("cash","bank"))),
            "fields" to listOf("name","type"),
            "order" to "name asc"
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Journal>>() {}.type
        gson.fromJson(result, type)
    }

    // ─── RETURNS ─────────────────────────────────────────────────────────────────

    suspend fun getReturns(): Result<List<Invoice>> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/account.move/search_read"
        val body = buildRpc("search_read", "account.move", kwargs = mapOf(
            "domain" to listOf(listOf("move_type", "=", "out_refund"), listOf("create_uid", "=", uid)),
            "fields" to listOf("name","move_type","state","partner_id","invoice_date","amount_total","payment_state","field_sales_ref"),
            "order" to "invoice_date desc",
            "limit" to 100
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Invoice>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun createReturn(partnerId: Int, linesData: List<Map<String, Any>>): Result<SimpleResult> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/create_field_credit_note"
        val body = buildRpc("create_field_credit_note", "res.partner", kwargs = mapOf(
            "partner_id" to partnerId,
            "lines_data" to linesData,
            "user_id" to uid
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Return creation failed")
        gson.fromJson(result, SimpleResult::class.java)
    }

    // ─── PRODUCTS ────────────────────────────────────────────────────────────────

    suspend fun getProducts(search: String = "", warehouseId: Int? = null): Result<List<Product>> = runCatching {
        val domain = mutableListOf<Any>(listOf("sale_ok", "=", true), listOf("active", "=", true))
        if (search.isNotBlank()) domain.add(listOf("name", "ilike", search))
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/product.product/search_read"
        val body = buildRpc("search_read", "product.product", kwargs = mapOf(
            "domain" to domain,
            "fields" to listOf("name","list_price","uom_id","uom_po_id","default_code","image_128","qty_available","taxes_id","barcode","categ_id"),
            "order" to "name asc",
            "limit" to 500
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Product>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun getWarehouses(): Result<List<Map<String, Any>>> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/stock.warehouse/search_read"
        val body = buildRpc("search_read", "stock.warehouse", kwargs = mapOf(
            "fields" to listOf("name","code"),
            "order" to "name asc"
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        gson.fromJson(result, type)
    }

    // ─── VISITS / ROUTES ─────────────────────────────────────────────────────────

    suspend fun getTodayRoute(): Result<DailyRoute> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/field.sales.daily.route/get_route_for_date"
        val body = buildRpc("get_route_for_date", "field.sales.daily.route", kwargs = mapOf("user_id" to uid))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("No route data")
        gson.fromJson(result, DailyRoute::class.java)
    }

    suspend fun startVisit(visitId: Int, lat: Double, lng: Double): Result<SimpleResult> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/field.sales.visit/start_visit"
        val body = buildRpc("start_visit", "field.sales.visit", kwargs = mapOf("visit_id" to visitId, "lat" to lat, "lng" to lng))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Start visit failed")
        gson.fromJson(result, SimpleResult::class.java)
    }

    suspend fun endVisit(visitId: Int, lat: Double, lng: Double, notes: String): Result<SimpleResult> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/field.sales.visit/end_visit"
        val body = buildRpc("end_visit", "field.sales.visit", kwargs = mapOf("visit_id" to visitId, "lat" to lat, "lng" to lng, "notes" to notes))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("End visit failed")
        gson.fromJson(result, SimpleResult::class.java)
    }

    // ─── ATTENDANCE ───────────────────────────────────────────────────────────────

    suspend fun getAttendanceStatus(): Result<AttendanceStatus> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/get_app_attendance_status"
        val body = buildRpc("get_app_attendance_status", "res.partner")
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("No attendance data")
        gson.fromJson(result, AttendanceStatus::class.java)
    }

    suspend fun checkIn(lat: Double, lng: Double): Result<SimpleResult> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/app_check_in"
        val body = buildRpc("app_check_in", "res.partner", kwargs = mapOf("lat" to lat, "long" to lng))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Check-in failed")
        gson.fromJson(result, SimpleResult::class.java)
    }

    suspend fun checkOut(lat: Double, lng: Double): Result<SimpleResult> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/app_check_out"
        val body = buildRpc("app_check_out", "res.partner", kwargs = mapOf("lat" to lat, "long" to lng))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Check-out failed")
        gson.fromJson(result, SimpleResult::class.java)
    }

    // ─── PRINT ───────────────────────────────────────────────────────────────────

    suspend fun getReportHtml(reportName: String, resId: Int): Result<String> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/field_sales/get_report_html"
        val body = OdooClient.buildAuthBody(
            mapOf("report_name" to reportName, "res_id" to resId)
        )
        val response = OdooClient.getService().call(url, body)
        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
        
        val odooRes = response.body() ?: throw Exception("No response")
        if (odooRes.error != null) throw Exception(odooRes.error.data?.message ?: odooRes.error.message ?: "RPC Error")
        
        val result = odooRes.result ?: throw Exception("Empty result")
        if (result.isJsonObject) {
            val obj = result.asJsonObject
            if (obj.has("html")) return@runCatching obj.get("html").asString
            if (obj.has("error")) throw Exception(obj.get("error").asString)
        }
        result.asString
    }

    // ─── EXPENSES ────────────────────────────────────────────────────────────────

    suspend fun getExpenses(): Result<List<Expense>> = runCatching {
        val uid = prefs.getUserId()
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/hr.expense/search_read"
        val body = buildRpc("search_read", "hr.expense", kwargs = mapOf(
            "domain" to listOf(listOf("create_uid", "=", uid)),
            "fields" to listOf("name","employee_id","product_id","unit_amount","quantity","total_amount","date","state","description"),
            "order" to "date desc",
            "limit" to 100
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: return@runCatching emptyList()
        val type = object : TypeToken<List<Expense>>() {}.type
        gson.fromJson(result, type)
    }

    suspend fun getCustomerStatement(partnerId: Int, startDate: String, endDate: String): Result<CustomerStatement> = runCatching {
        val url = prefs.getOdooUrl().trimEnd('/') + "/web/dataset/call_kw/res.partner/get_customer_statement"
        val body = buildRpc("get_customer_statement", "res.partner", kwargs = mapOf(
            "partner_id" to partnerId, "start_date" to startDate, "end_date" to endDate
        ))
        val response = OdooClient.getService().call(url, body)
        val result = response.body()?.result ?: throw Exception("Statement failed")
        gson.fromJson(result, CustomerStatement::class.java)
    }
}

package com.tts.fieldsales.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Helper extensions for Odoo's inconsistent ManyToOne fields (List [id, name] or Boolean false)
fun JsonElement?.toOdooName(fallback: String = "-"): String {
    if (this == null || (this.isJsonPrimitive && this.asJsonPrimitive.isBoolean)) return fallback
    if (this.isJsonArray) {
        val arr = this.asJsonArray
        if (arr.size() >= 2) return arr.get(1).asString
    }
    return fallback
}

fun JsonElement?.toOdooId(): Int? {
    if (this == null || (this.isJsonPrimitive && this.asJsonPrimitive.isBoolean)) return null
    if (this.isJsonArray) {
        val arr = this.asJsonArray
        if (arr.size() >= 1) return arr.get(0).asInt
    }
    if (this.isJsonPrimitive && this.asJsonPrimitive.isNumber) return this.asInt
    return null
}

// ===================== ODOO JSON-RPC BASE =====================
data class OdooRequest(
    val jsonrpc: String = "2.0",
    val method: String = "call",
    val id: Int = 1,
    val params: Any
)

data class OdooResponse<T>(
    val jsonrpc: String?,
    val id: Int?,
    val result: T?,
    val error: OdooError?
)

data class OdooError(
    val code: Int?,
    val message: String?,
    val data: OdooErrorData?
)

data class OdooErrorData(
    val name: String?,
    val message: String?,
    val arguments: List<String>?
)

// ===================== AUTH =====================
data class LoginParams(
    val db: String,
    val login: String,
    val password: String
)

data class LoginResult(
    val uid: Int?,
    val name: String?,
    val username: String?,
    @SerializedName("company_id") val companyId: JsonElement?,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("is_admin") val isAdmin: Boolean?
)

// ===================== USER =====================
data class UserDetails(
    val id: Int,
    val name: String,
    @SerializedName("field_sales_pin") val pin: String?,
    @SerializedName("image_128") val image128: String?,
    @SerializedName("field_sales_global_access") val globalAccess: Boolean?,
    @SerializedName("field_sales_can_see_customers") val canSeeCustomers: Boolean?,
    @SerializedName("field_sales_can_see_orders") val canSeeOrders: Boolean?,
    @SerializedName("field_sales_can_see_invoices") val canSeeInvoices: Boolean?,
    @SerializedName("field_sales_can_see_returns") val canSeeReturns: Boolean?,
    @SerializedName("field_sales_can_see_payments") val canSeePayments: Boolean?,
    @SerializedName("field_sales_can_see_visits") val canSeeVisits: Boolean?,
    @SerializedName("field_sales_can_see_expenses") val canSeeExpenses: Boolean?,
    @SerializedName("field_sales_can_see_closing") val canSeeClosing: Boolean?,
    @SerializedName("field_sales_can_see_routes") val canSeeRoutes: Boolean?,
    @SerializedName("field_sales_allowed_warehouse_ids") val allowedWarehouseIds: List<Int>?,
    @SerializedName("field_sales_allowed_journal_ids") val allowedJournalIds: List<Int>?,
    @SerializedName("field_sales_allowed_brand_ids") val allowedBrandIds: List<Int>?,
    @SerializedName("field_sales_daily_goal") val dailyGoal: Double?
)

// ===================== CUSTOMER =====================
data class Partner(
    val id: Int,
    val name: String,
    val ref: String?,
    val street: String?,
    val city: String?,
    val phone: String?,
    val mobile: String?,
    val email: String?,
    val vat: String?,
    @SerializedName("partner_latitude") val latitude: Double?,
    @SerializedName("partner_longitude") val longitude: Double?,
    @SerializedName("arabic_customer_name") val arabicName: String?,
    @SerializedName("approval_status") val approvalStatus: String?,
    val credit: Double?,
    @SerializedName("credit_limit") val creditLimit: Double?,
    @SerializedName("image_128") val image128: String?,
    @SerializedName("property_product_pricelist") val pricelistId: JsonElement?,
    @SerializedName("total_overdue") val totalOverdue: Double?
)

// ===================== PRODUCT =====================
data class Product(
    val id: Int,
    val name: String,
    @SerializedName("list_price") val listPrice: Double,
    @SerializedName("standard_price") val costPrice: Double?,
    @SerializedName("uom_id") val uomId: JsonElement?,
    @SerializedName("uom_po_id") val uomPoId: JsonElement?,
    @SerializedName("categ_id") val categId: JsonElement?,
    @SerializedName("default_code") val defaultCode: String?,
    @SerializedName("image_128") val image128: String?,
    @SerializedName("qty_available") val qtyAvailable: Double?,
    @SerializedName("taxes_id") val taxIds: List<Int>?,
    val barcode: String?,
    val description: String?,
    @SerializedName("brand_id") val brandId: JsonElement?,
    @SerializedName("product_tmpl_id") val productTmplId: JsonElement?
)

data class UomUnit(
    val id: Int,
    val name: String,
    val category: String?
)

// ===================== SALE ORDER =====================
data class SaleOrder(
    val id: Int,
    val name: String,
    val state: String,
    @SerializedName("date_order") val dateOrder: String?,
    @SerializedName("partner_id") val partnerId: JsonElement?,
    @SerializedName("user_id") val userId: JsonElement?,
    @SerializedName("amount_untaxed") val amountUntaxed: Double,
    @SerializedName("amount_tax") val amountTax: Double,
    @SerializedName("amount_total") val amountTotal: Double,
    @SerializedName("order_line") val orderLineIds: List<Int>?,
    @SerializedName("invoice_ids") val invoiceIds: List<Int>?,
    @SerializedName("invoice_status") val invoiceStatus: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("warehouse_id") val warehouseId: JsonElement?,
    @SerializedName("company_id") val companyId: JsonElement?
)

data class SaleOrderLine(
    val id: Int,
    @SerializedName("order_id") val orderId: JsonElement?,
    @SerializedName("product_id") val productId: JsonElement?,
    val name: String?,
    @SerializedName("product_uom_qty") val qty: Double,
    @SerializedName("product_uom") val uomId: JsonElement?,
    @SerializedName("price_unit") val priceUnit: Double,
    val discount: Double?,
    @SerializedName("price_subtotal") val priceSubtotal: Double,
    @SerializedName("price_total") val priceTotal: Double,
    @SerializedName("tax_id") val taxIds: List<Int>?,
    @SerializedName("display_type") val displayType: String?
)

// ===================== INVOICE =====================
data class Invoice(
    val id: Int,
    val name: String?,
    @SerializedName("move_type") val moveType: String,
    val state: String,
    @SerializedName("partner_id") val partnerId: JsonElement?,
    @SerializedName("invoice_date") val invoiceDate: String?,
    @SerializedName("invoice_date_due") val invoiceDateDue: String?,
    @SerializedName("amount_untaxed") val amountUntaxed: Double,
    @SerializedName("amount_tax") val amountTax: Double,
    @SerializedName("amount_total") val amountTotal: Double,
    @SerializedName("amount_residual") val amountResidual: Double,
    @SerializedName("invoice_user_id") val userId: List<Any>?,
    @SerializedName("ref") val ref: String?,
    @SerializedName("invoice_line_ids") val lineIds: List<Int>?,
    @SerializedName("payment_state") val paymentState: String?,
    @SerializedName("company_id") val companyId: JsonElement?,
    @SerializedName("field_sales_ref") val fieldSalesRef: String?
)

data class InvoiceLine(
    val id: Int,
    @SerializedName("product_id") val productId: JsonElement?,
    val name: String?,
    val quantity: Double,
    @SerializedName("price_unit") val priceUnit: Double,
    @SerializedName("price_subtotal") val priceSubtotal: Double,
    @SerializedName("price_total") val priceTotal: Double,
    val discount: Double?,
    @SerializedName("product_uom_id") val uomId: JsonElement?,
    @SerializedName("tax_ids") val taxIds: List<Int>?,
    @SerializedName("display_type") val displayType: String?
)

// ===================== PAYMENT =====================
data class Payment(
    val id: Int,
    val name: String?,
    @SerializedName("partner_id") val partnerId: JsonElement?,
    val amount: Double,
    val date: String?,
    @SerializedName("journal_id") val journalId: JsonElement?,
    val state: String,
    @SerializedName("field_sales_status") val fieldSalesStatus: String?,
    @SerializedName("field_sales_ref") val fieldSalesRef: String?,
    @SerializedName("create_uid") val createUid: JsonElement?,
    @SerializedName("company_id") val companyId: JsonElement?,
    @SerializedName("currency_id") val currencyId: JsonElement?
)

data class Journal(
    val id: Int,
    val name: String,
    val type: String?,
    @SerializedName("company_id") val companyId: JsonElement?
)

// ===================== VISIT =====================
data class Visit(
    val id: Int,
    val name: String,
    @SerializedName("partner_id") val partnerId: JsonElement?,
    @SerializedName("user_id") val userId: JsonElement?,
    @SerializedName("date_planned") val datePlanned: String?,
    val state: String,
    @SerializedName("visit_type") val visitType: String?,
    @SerializedName("visit_notes") val visitNotes: String?,
    @SerializedName("checkin_date") val checkinDate: String?,
    @SerializedName("checkout_date") val checkoutDate: String?,
    @SerializedName("checkin_latitude") val checkinLat: Double?,
    @SerializedName("checkin_longitude") val checkinLng: Double?,
    @SerializedName("actual_duration") val actualDuration: Double?,
    @SerializedName("allow_remote_checkin") val allowRemoteCheckin: Boolean?,
    @SerializedName("daily_route_id") val dailyRouteId: JsonElement?
)

data class VisitData(
    val id: Int,
    @SerializedName("customer_id") val customerId: Int?,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("customer_code") val customerCode: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("visit_type") val visitType: String?,
    val state: String?,
    @SerializedName("is_self_visit") val isSelfVisit: Boolean?,
    val notes: String?,
    @SerializedName("actual_start_time") val actualStartTime: String?,
    @SerializedName("actual_end_time") val actualEndTime: String?,
    @SerializedName("checkin_date") val checkinDate: String?,
    @SerializedName("checkout_date") val checkoutDate: String?,
    @SerializedName("customer_image") val customerImage: String?,
    @SerializedName("allow_remote_checkin") val allowRemoteCheckin: Boolean?,
    @SerializedName("distance_from_user") val distanceFromUser: Double?
)

// ===================== ROUTE =====================
data class DailyRoute(
    val id: Int,
    val name: String?,
    val date: String?,
    @SerializedName("day_of_week") val dayOfWeek: String?,
    val state: String?,
    @SerializedName("total_visits") val totalVisits: Int?,
    @SerializedName("completed_visits") val completedVisits: Int?,
    @SerializedName("assigned_visits") val assignedVisits: Int?,
    @SerializedName("self_visits") val selfVisits: Int?,
    @SerializedName("progress_percentage") val progressPercentage: Double?,
    @SerializedName("total_distance") val totalDistance: String?,
    @SerializedName("estimated_time") val estimatedTime: String?,
    val visits: List<VisitData>?
)

// ===================== EXPENSE =====================
data class Expense(
    val id: Int,
    val name: String,
    @SerializedName("employee_id") val employeeId: JsonElement?,
    @SerializedName("product_id") val productId: JsonElement?,
    @SerializedName("unit_amount") val unitAmount: Double,
    val quantity: Double,
    @SerializedName("total_amount") val totalAmount: Double?,
    val date: String?,
    val state: String,
    val description: String?,
    @SerializedName("company_id") val companyId: JsonElement?
)

// ===================== DASHBOARD =====================
data class DashboardStats(
    val day: String,
    val total: Double,
    val height: String
)

data class PerformanceData(
    @SerializedName("daily_goal") val dailyGoal: Double,
    @SerializedName("today_sales") val todaySales: Double,
    @SerializedName("goal_progress") val goalProgress: Double,
    @SerializedName("formatted_goal") val formattedGoal: String,
    @SerializedName("formatted_sales") val formattedSales: String,
    @SerializedName("chart_data") val chartData: List<DashboardStats>?
)

// ===================== ATTENDANCE =====================
data class AttendanceStatus(
    val status: String,
    @SerializedName("employee_name") val employeeName: String?,
    @SerializedName("check_in_time") val checkInTime: String?,
    val message: String?
)

// ===================== CART =====================
data class CartItem(
    val productId: Int,
    val productName: String,
    val productImage: String?,
    val uomId: Int,
    val uomName: String,
    var qty: Double,
    var priceUnit: Double,
    var discount: Double,
    val taxIds: List<Int>
) {
    val subtotal: Double get() = qty * priceUnit * (1 - discount / 100)
}

// ===================== CUSTOMER STATEMENT =====================
data class StatementLine(
    val id: Int,
    val date: String?,
    val ref: String?,
    val name: String?,
    val debit: Double,
    val credit: Double,
    val balance: Double
)

data class CustomerStatement(
    val currency: String?,
    @SerializedName("partner_name") val partnerName: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("opening_balance") val openingBalance: Double,
    val lines: List<StatementLine>?,
    @SerializedName("closing_balance") val closingBalance: Double,
    @SerializedName("total_overdue") val totalOverdue: Double?,
    @SerializedName("total_due") val totalDue: Double?,
    val company: Map<String, Any>?,
    val error: String?
)

// ===================== MISC =====================
data class SimpleResult(
    val success: Boolean?,
    val error: String?,
    val state: String?,
    val message: String?,
    val id: Int?,
    val name: String?,
    @SerializedName("invoice_id") val invoiceId: Int?,
    @SerializedName("html") val html: String?,
    @SerializedName("visit_id") val visitId: Int?,
    @SerializedName("pdf_base64") val pdfBase64: String?,
    val route: DailyRoute?
)

data class PriceResult(
    val prices: Map<String, Double>?
)

data class DueDetail(
    val id: Int,
    val name: String,
    val ref: String?,
    @SerializedName("total_due") val totalDue: Double,
    @SerializedName("max_days") val maxDays: Int
)

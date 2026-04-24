package com.tts.fieldsales.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tts.fieldsales.data.model.*
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── ORDERS ──────────────────────────────────────────────────────────────────

data class OrdersState(
    val orders: List<SaleOrder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrdersViewModel : ViewModel() {
    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true, error = null) }
        repo.getSaleOrders().fold(
            onSuccess = { list -> _state.update { it.copy(orders = list, isLoading = false) } },
            onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        )
    }

    fun submitApproval(context: Context, orderId: Int, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.submitOrderForApproval(orderId).fold(onSuccess = { onDone(true) }, onFailure = { onDone(false) })
    }

    fun confirmOrder(context: Context, orderId: Int, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.confirmOrder(orderId).fold(onSuccess = { onDone(true) }, onFailure = { onDone(false) })
    }

    fun createInvoice(context: Context, orderId: Int, onDone: (List<Int>) -> Unit) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.createInvoiceFromOrder(orderId).fold(onSuccess = { onDone(it) }, onFailure = { onDone(emptyList()) })
    }
}

// ─── ORDER DETAIL ────────────────────────────────────────────────────────────

data class OrderDetailState(
    val order: SaleOrder? = null,
    val lines: List<SaleOrderLine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

class OrderDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailState())
    val state: StateFlow<OrderDetailState> = _state.asStateFlow()

    fun load(context: Context, orderId: Int) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true, error = null) }
        
        repo.getSaleOrders().onSuccess { list ->
            val order = list.firstOrNull { it.id == orderId }
            if (order != null) {
                repo.getOrderLines(orderId).onSuccess { lines ->
                    _state.update { it.copy(order = order, lines = lines, isLoading = false) }
                }.onFailure { e ->
                    _state.update { it.copy(order = order, lines = emptyList(), isLoading = false, error = "Lines: ${e.message}") }
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Order #$orderId not found.") }
            }
        }.onFailure { e ->
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun submitApproval(context: Context, orderId: Int) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.submitOrderForApproval(orderId).fold(
            onSuccess = { _state.update { s -> s.copy(actionMessage = "Order submitted for approval.") }; load(context, orderId) },
            onFailure = { e -> _state.update { s -> s.copy(actionMessage = "Failed: ${e.message}") } }
        )
    }

    fun confirmOrder(context: Context, orderId: Int) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.confirmOrder(orderId).fold(
            onSuccess = { _state.update { s -> s.copy(actionMessage = "Order confirmed!") }; load(context, orderId) },
            onFailure = { e -> _state.update { s -> s.copy(actionMessage = "Failed: ${e.message}") } }
        )
    }

    fun createInvoice(context: Context, orderId: Int) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.createInvoiceFromOrder(orderId).fold(
            onSuccess = { ids -> _state.update { s -> s.copy(actionMessage = if (ids.isNotEmpty()) "Invoice created!" else "Invoice already exists.") } },
            onFailure = { e -> _state.update { s -> s.copy(actionMessage = "Failed: ${e.message}") } }
        )
    }

    fun clearMessage() = _state.update { it.copy(actionMessage = null) }
}

// ─── INVOICES ────────────────────────────────────────────────────────────────

data class InvoicesState(val invoices: List<Invoice> = emptyList(), val isLoading: Boolean = false, val error: String? = null)

class InvoicesViewModel : ViewModel() {
    private val _state = MutableStateFlow(InvoicesState())
    val state: StateFlow<InvoicesState> = _state.asStateFlow()

    fun load(context: Context, moveType: String = "out_invoice") = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true, error = null) }
        repo.getInvoices(moveType).fold(
            onSuccess = { list -> _state.update { it.copy(invoices = list, isLoading = false) } },
            onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        )
    }
}

// ─── PAYMENTS ────────────────────────────────────────────────────────────────

data class PaymentsState(
    val payments: List<Payment> = emptyList(),
    val journals: List<Journal> = emptyList(),
    val customers: List<Partner> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

class PaymentsViewModel : ViewModel() {
    private val _state = MutableStateFlow(PaymentsState())
    val state: StateFlow<PaymentsState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true) }
        repo.getPayments().onSuccess { p -> _state.update { it.copy(payments = p) } }
        repo.getJournals().onSuccess { j -> _state.update { it.copy(journals = j) } }
        repo.getCustomers().onSuccess { c -> _state.update { it.copy(customers = c) } }
        _state.update { it.copy(isLoading = false) }
    }

    fun createPayment(context: Context, partnerId: Int, amount: Double, journalId: Int, memo: String) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.createPayment(partnerId, amount, journalId, memo).fold(
            onSuccess = { _state.update { s -> s.copy(actionMessage = "Payment recorded!") }; load(context) },
            onFailure = { e -> _state.update { s -> s.copy(actionMessage = "Failed: ${e.message}") } }
        )
    }

    fun clearMessage() = _state.update { it.copy(actionMessage = null) }
}

// ─── CUSTOMERS ───────────────────────────────────────────────────────────────

data class CustomersState(val customers: List<Partner> = emptyList(), val isLoading: Boolean = false, val error: String? = null)

class CustomersViewModel : ViewModel() {
    private val _state = MutableStateFlow(CustomersState())
    val state: StateFlow<CustomersState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true) }
        repo.getCustomers().fold(
            onSuccess = { _state.update { s -> s.copy(customers = it, isLoading = false) } },
            onFailure = { _state.update { s -> s.copy(isLoading = false, error = it.message) } }
        )
    }

    fun search(context: Context, query: String) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.getCustomers(query).onSuccess { _state.update { s -> s.copy(customers = it) } }
    }
}

// ─── ROUTES / VISITS ────────────────────────────────────────────────────────

data class RouteState(val route: DailyRoute? = null, val isLoading: Boolean = false, val error: String? = null, val actionMsg: String? = null)

class RouteViewModel : ViewModel() {
    private val _state = MutableStateFlow(RouteState())
    val state: StateFlow<RouteState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true) }
        repo.getTodayRoute().fold(
            onSuccess = { _state.update { s -> s.copy(route = it, isLoading = false) } },
            onFailure = { _state.update { s -> s.copy(isLoading = false, error = it.message) } }
        )
    }

    fun startVisit(context: Context, visitId: Int, lat: Double, lng: Double) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.startVisit(visitId, lat, lng).fold(
            onSuccess = { _state.update { s -> s.copy(actionMsg = "Visit started!") }; load(context) },
            onFailure = { e -> _state.update { s -> s.copy(actionMsg = "Failed: ${e.message}") } }
        )
    }

    fun endVisit(context: Context, visitId: Int, lat: Double, lng: Double, notes: String) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.endVisit(visitId, lat, lng, notes).fold(
            onSuccess = { _state.update { s -> s.copy(actionMsg = "Visit completed!") }; load(context) },
            onFailure = { e -> _state.update { s -> s.copy(actionMsg = "Failed: ${e.message}") } }
        )
    }

    fun clearMsg() = _state.update { it.copy(actionMsg = null) }
}

// ─── ATTENDANCE ───────────────────────────────────────────────────────────────

data class AttendanceState(
    val status: String = "unknown",
    val employeeName: String = "",
    val checkInTime: String? = null,
    val isLoading: Boolean = false,
    val actionMsg: String? = null
)

class AttendanceViewModel : ViewModel() {
    private val _state = MutableStateFlow(AttendanceState())
    val state: StateFlow<AttendanceState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true) }
        repo.getAttendanceStatus().fold(
            onSuccess = { att -> _state.update { it.copy(status = att.status, employeeName = att.employeeName ?: "", checkInTime = att.checkInTime, isLoading = false) } },
            onFailure = { _state.update { it.copy(isLoading = false) } }
        )
    }

    fun checkIn(context: Context, lat: Double, lng: Double) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.checkIn(lat, lng).fold(
            onSuccess = { r -> _state.update { s -> s.copy(actionMsg = r.message ?: "Checked in!") }; load(context) },
            onFailure = { e -> _state.update { s -> s.copy(actionMsg = "Failed: ${e.message}") } }
        )
    }

    fun checkOut(context: Context, lat: Double, lng: Double) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        repo.checkOut(lat, lng).fold(
            onSuccess = { r -> _state.update { s -> s.copy(actionMsg = r.message ?: "Checked out!") }; load(context) },
            onFailure = { e -> _state.update { s -> s.copy(actionMsg = "Failed: ${e.message}") } }
        )
    }

    fun clearMsg() = _state.update { it.copy(actionMsg = null) }
}

// ─── EXPENSES ────────────────────────────────────────────────────────────────

data class ExpensesState(val expenses: List<Expense> = emptyList(), val isLoading: Boolean = false)

class ExpensesViewModel : ViewModel() {
    private val _state = MutableStateFlow(ExpensesState())
    val state: StateFlow<ExpensesState> = _state.asStateFlow()

    fun load(context: Context) = viewModelScope.launch {
        val repo = OdooRepository(AppPreferences(context))
        _state.update { it.copy(isLoading = true) }
        repo.getExpenses().fold(
            onSuccess = { _state.update { s -> s.copy(expenses = it, isLoading = false) } },
            onFailure = { _state.update { s -> s.copy(isLoading = false) } }
        )
    }
}

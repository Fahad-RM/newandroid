package com.tts.fieldsales.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tts.fieldsales.data.model.DashboardStats
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val userName: String = "",
    val todaySales: Double = 0.0,
    val dailyGoal: Double = 1000.0,
    val goalProgress: Float = 0f,
    val todayOrders: Int = 0,
    val visitsCompleted: Int = 0,
    val totalVisits: Int = 0,
    val pendingApprovals: Int = 0,
    val isCheckedIn: Boolean = false,
    val chartData: List<DashboardStats> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun load(context: Context) {
        val prefs = AppPreferences(context)
        val repo = OdooRepository(prefs)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, userName = prefs.getUsername()) }
            // Load performance data
            repo.getPerformanceData().onSuccess { perf ->
                _state.update {
                    it.copy(
                        todaySales = perf.todaySales,
                        dailyGoal = perf.dailyGoal,
                        goalProgress = (perf.goalProgress / 100f).toFloat(),
                        chartData = perf.chartData ?: emptyList(),
                        isLoading = false
                    )
                }
            }.onFailure {
                _state.update { s -> s.copy(isLoading = false) }
            }
            // Load attendance
            repo.getAttendanceStatus().onSuccess { att ->
                _state.update { it.copy(isCheckedIn = att.status == "checked_in") }
            }
            // Load route stats
            repo.getTodayRoute().onSuccess { route ->
                _state.update {
                    it.copy(
                        visitsCompleted = route.completedVisits ?: 0,
                        totalVisits = route.totalVisits ?: 0
                    )
                }
            }
        }
    }
}

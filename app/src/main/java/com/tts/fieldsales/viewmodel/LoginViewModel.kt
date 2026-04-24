package com.tts.fieldsales.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tts.fieldsales.data.api.OdooClient
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.data.repository.OdooRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoginState(
    val odooUrl: String = "",
    val database: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun setUrl(url: String) = _state.update { it.copy(odooUrl = url, errorMessage = null) }
    fun setDatabase(db: String) = _state.update { it.copy(database = db, errorMessage = null) }
    fun setUsername(u: String) = _state.update { it.copy(username = u, errorMessage = null) }
    fun setPassword(p: String) = _state.update { it.copy(password = p, errorMessage = null) }

    fun login(context: Context) {
        val s = _state.value
        if (s.odooUrl.isBlank() || s.database.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }
        val prefs = AppPreferences(context)
        val repo = OdooRepository(prefs)

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repo.login(s.odooUrl.trim(), s.database.trim(), s.username.trim(), s.password)
            result.fold(
                onSuccess = { uid ->
                    _state.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message?.take(120) ?: "Login failed. Check your credentials."
                        )
                    }
                }
            )
        }
    }

    fun checkExistingLogin(context: Context) {
        viewModelScope.launch {
            val prefs = AppPreferences(context)
            if (prefs.isLoggedIn()) {
                val url = prefs.getOdooUrl()
                if (url.isNotBlank()) {
                    OdooClient.initialize(url)
                    _state.update { it.copy(isLoggedIn = true) }
                }
            }
        }
    }
}

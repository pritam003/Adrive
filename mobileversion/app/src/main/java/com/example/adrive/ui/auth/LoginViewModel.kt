package com.example.adrive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adrive.data.model.DeviceCodeInfo
import com.example.adrive.data.model.MeResponse
import com.example.adrive.data.repository.DriveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Checking : AuthState()
    data object Unauthenticated : AuthState()
    data class DeviceCodeFlow(val info: DeviceCodeInfo, val polling: Boolean) : AuthState()
    data class Authenticated(val me: MeResponse) : AuthState()
    data class AccessDenied(val userDetails: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class LoginViewModel : ViewModel() {

    private val repo = DriveRepository()

    private val _state = MutableStateFlow<AuthState>(AuthState.Checking)
    val state: StateFlow<AuthState> = _state

    private var pollJob: Job? = null

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            if (repo.isLoggedIn()) {
                repo.getMe().fold(
                    onSuccess = { me -> handleMe(me) },
                    onFailure = { _state.value = AuthState.Unauthenticated }
                )
            } else {
                _state.value = AuthState.Unauthenticated
            }
        }
    }

    private fun handleMe(me: MeResponse) {
        when {
            !me.authenticated -> _state.value = AuthState.Unauthenticated
            me.ownerConfigured && me.isOwner == false ->
                _state.value = AuthState.AccessDenied(me.userDetails)
            else -> _state.value = AuthState.Authenticated(me)
        }
    }

    fun startLogin() {
        _state.value = AuthState.Checking
        viewModelScope.launch {
            repo.startDeviceLogin().fold(
                onSuccess = { info ->
                    _state.value = AuthState.DeviceCodeFlow(info, polling = false)
                    beginPolling(info)
                },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    private fun beginPolling(info: DeviceCodeInfo) {
        val expiresAt = System.currentTimeMillis() + info.expiresIn * 1000L
        val intervalMs = maxOf(info.interval, 2) * 1000L

        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _state.value = AuthState.DeviceCodeFlow(info, polling = true)
            while (true) {
                delay(intervalMs)
                if (System.currentTimeMillis() > expiresAt) {
                    _state.value = AuthState.Error("Code expired. Please try again.")
                    return@launch
                }
                repo.pollDeviceLogin(info.deviceCode).fold(
                    onSuccess = { resp ->
                        when (resp.status) {
                            "success" -> {
                                repo.getMe().fold(
                                    onSuccess = ::handleMe,
                                    onFailure = { _state.value = AuthState.Error(it.message ?: "") }
                                )
                                return@launch
                            }
                            "expired" -> {
                                _state.value = AuthState.Error("Code expired. Please try again.")
                                return@launch
                            }
                            "error" -> {
                                _state.value = AuthState.Error(resp.error ?: "Login failed")
                                return@launch
                            }
                            // "pending" → keep polling
                        }
                    },
                    onFailure = { /* network blip – keep polling */ }
                )
            }
        }
    }

    fun cancelLogin() {
        pollJob?.cancel()
        _state.value = AuthState.Unauthenticated
    }

    fun signOut() {
        viewModelScope.launch {
            repo.logout()
            pollJob?.cancel()
            _state.value = AuthState.Unauthenticated
        }
    }
}


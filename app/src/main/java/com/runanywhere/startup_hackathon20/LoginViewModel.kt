package com.runanywhere.startup_hackathon20

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _loginState = mutableStateOf<LoginState>(LoginState.Idle)
    val loginState: State<LoginState> = _loginState

    fun login() {
        _loginState.value = LoginState.Loading
        // Simulate a network call
        _loginState.value = LoginState.Success
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}

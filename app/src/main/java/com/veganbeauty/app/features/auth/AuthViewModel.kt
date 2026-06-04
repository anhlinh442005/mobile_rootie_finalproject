package com.veganbeauty.app.features.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> get() = _loginState

    private val _registerState = MutableLiveData<AuthState>()
    val registerState: LiveData<AuthState> get() = _registerState

    fun login(emailOrPhone: String, password: String) {
        if (emailOrPhone.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("Vui lòng nhập đầy đủ thông tin.")
            return
        }

        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = authRepository.login(emailOrPhone, password)
                if (user != null) {
                    _loginState.value = AuthState.Success(user)
                } else {
                    _loginState.value = AuthState.Error("Tài khoản hoặc mật khẩu không đúng.")
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("Đã xảy ra lỗi: ${e.message}")
            }
        }
    }

    fun register(fullName: String, emailOrPhone: String, password: String) {
        if (fullName.isBlank() || emailOrPhone.isBlank() || password.isBlank()) {
            _registerState.value = AuthState.Error("Vui lòng nhập đầy đủ thông tin.")
            return
        }

        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.register(fullName, emailOrPhone, password)
                if (result.isSuccess) {
                    _registerState.value = AuthState.Success(result.getOrNull()!!)
                } else {
                    _registerState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Đăng ký thất bại.")
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error("Đã xảy ra lỗi: ${e.message}")
            }
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val user: UserEntity) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}

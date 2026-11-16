package app.pantauizin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.pantauizin.data.firebase.AuthManager
import app.pantauizin.data.firebase.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val kataSandi: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail, errorMessage = null)
    }

    fun onKataSandiChange(newPass: String) {
        _uiState.value = _uiState.value.copy(kataSandi = newPass, errorMessage = null)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null
        )
    }

    fun submit(onSuccess: (UserProfile) -> Unit) {
        val email = _uiState.value.email.trim()
        val pass = _uiState.value.kataSandi.trim()

        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email tidak boleh kosong")
            return
        }
        if (pass.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kata sandi tidak boleh kosong")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (_uiState.value.isRegisterMode) {
                authManager.signUp(email, pass)
            } else {
                authManager.signIn(email, pass)
            }
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: if (_uiState.value.isRegisterMode) "Gagal membuat akun" else "Gagal masuk"
                    )
                }
            )
        }
    }

    fun login(onSuccess: (UserProfile) -> Unit) = submit(onSuccess)

    companion object {
        fun provideFactory(authManager: AuthManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(authManager) as T
            }
    }
}

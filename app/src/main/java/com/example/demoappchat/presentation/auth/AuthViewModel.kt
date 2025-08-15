package com.example.demoappchat.presentation.auth


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demoappchat.data.model.User
import com.example.demoappchat.data.repository.FirebaseRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = repository.currentUser

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.registerUser(email, password, name)
                .onSuccess { user ->
                    Log.d("AuthViewModel", "✅ Registro exitoso: ${user.name}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { exception ->
                    Log.d("AuthViewModel", "❌ Error en signUp: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido"
                    )
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.loginUser(email, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido"
                    )
                }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val result = repository.signInWithGoogle(credential)
                
                result.onSuccess { user ->
                    Log.d("AuthViewModel", "✅ Login con Google exitoso: ${user.name}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "❌ Error en login con Google: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al iniciar sesión con Google"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Error procesando cuenta de Google", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al procesar la cuenta de Google"
                )
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Intenta registrar o hacer login automáticamente
     * Útil cuando el usuario no está seguro si ya tiene cuenta
     */
    fun signUpOrSignIn(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.registerOrLogin(email, password, name)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido"
                    )
                }
        }
    }
    
    /**
     * Verifica si un email ya está registrado
     */
    suspend fun checkEmailExists(email: String): Boolean {
        return repository.isEmailRegistered(email)
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)
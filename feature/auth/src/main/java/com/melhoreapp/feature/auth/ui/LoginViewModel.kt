package com.melhoreapp.feature.auth.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.common.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _signInState = MutableStateFlow<Result<com.melhoreapp.core.auth.CurrentUser>?>(null)
    val signInState: StateFlow<Result<com.melhoreapp.core.auth.CurrentUser>?> = _signInState.asStateFlow()

    fun getSignInIntent(context: Context): Intent =
        authRepository.getSignInIntent(context)

    fun handleSignInResult(resultCode: Int, data: Intent?) {
        scope.launch {
            authRepository.signInWithSignInResult(resultCode, data).collect { result ->
                _signInState.update { result }
            }
        }
    }

    fun clearSignInState() {
        _signInState.update { null }
    }
}

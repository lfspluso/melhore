package com.melhoreapp.core.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.melhoreapp.core.common.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInOptions: com.google.android.gms.auth.api.signin.GoogleSignInOptions,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(context.applicationContext, googleSignInOptions)
    }

    val currentUser: StateFlow<CurrentUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toCurrentUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toCurrentUser())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, firebaseAuth.currentUser?.toCurrentUser())

    fun getSignInIntent(context: Context): Intent {
        return googleSignInClient.signInIntent
    }

    fun signInWithSignInResult(resultCode: Int, data: Intent?): Flow<Result<CurrentUser>> = flow {
        emit(Result.Loading)
        if (data == null) {
            emit(Result.Error(NullPointerException("Sign-in result data is null")))
            return@flow
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account: GoogleSignInAccount? = try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            emit(Result.Error(e))
            return@flow
        }
        val idToken = account?.idToken
        if (idToken == null) {
            emit(Result.Error(NullPointerException("ID token is null")))
            return@flow
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user?.toCurrentUser()
            if (user != null) {
                emit(Result.Success(user))
            } else {
                emit(Result.Error(NullPointerException("Firebase user is null after sign-in")))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    fun signOut(): Flow<Result<Unit>> = flow {
        try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toCurrentUser(): CurrentUser =
        CurrentUser(
            userId = uid,
            email = email
        )
}

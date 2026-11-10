package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthUserState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FirebaseAuthManager private constructor(private val appContext: Context) {

    private val auth: FirebaseAuth get() = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    private val _userState = MutableStateFlow(AuthUserState(user = auth.currentUser))
    val userState: StateFlow<AuthUserState> = _userState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userState.value = _userState.value.copy(user = firebaseAuth.currentUser)
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        _userState.value = _userState.value.copy(isLoading = true, errorMessage = null)

        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val error = "Google Sign-In configuration error: default_web_client_id not found."
            Log.e(TAG, error, e)
            _userState.value = _userState.value.copy(isLoading = false, errorMessage = error)
            return Result.failure(IllegalStateException(error, e))
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: error("Firebase sign-in succeeded but returned null user")

                // Sync profile to Firestore
                syncUserProfileToFirestore(user)

                _userState.value = AuthUserState(user = user, isLoading = false, errorMessage = null)
                Log.d(TAG, "Successfully signed in via Google Firebase Auth: ${user.email} (${user.uid})")
                Result.success(user)
            } else {
                val error = "Unsupported credential format received from Google Play services."
                _userState.value = _userState.value.copy(isLoading = false, errorMessage = error)
                Result.failure(IllegalStateException(error))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed by user: ${e.message}", e)
            _userState.value = _userState.value.copy(isLoading = false)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val message = e.localizedMessage ?: "Sign in failed"
            _userState.value = _userState.value.copy(isLoading = false, errorMessage = message)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return withContext(NonCancellable + Dispatchers.IO) {
            try {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed clearing CredentialManager state: ${e.message}")
                }
                auth.signOut()
                _userState.value = AuthUserState(user = null, isLoading = false, errorMessage = null)
                Log.d(TAG, "Successfully signed out from Firebase Auth")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sign out error", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun syncUserProfileToFirestore(user: FirebaseUser) {
        withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance(
                    appContext.getString(R.string.firestore_database_id)
                )

                val profileData = hashMapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName ?: "User"),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "lastLogin" to FieldValue.serverTimestamp()
                )

                db.collection("users")
                    .document(user.uid)
                    .set(profileData, SetOptions.merge())
                    .await()

                Log.d(TAG, "User profile successfully synced to Firestore for UID: ${user.uid}")
            } catch (e: Exception) {
                Log.w(TAG, "Non-fatal: could not sync user profile to Firestore: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"

        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

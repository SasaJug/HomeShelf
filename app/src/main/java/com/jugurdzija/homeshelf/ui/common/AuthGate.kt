package com.jugurdzija.homeshelf.ui.common

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.jugurdzija.homeshelf.R

private const val TAG = "AuthGate"

@Composable
fun AuthGate(content: @Composable (onLogout: () -> Unit) -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var authenticated by remember { mutableStateOf(auth.currentUser != null) }
    var signInError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val isSignedIn = firebaseAuth.currentUser != null
            Log.d(TAG, "AuthStateListener fired, isSignedIn=$isSignedIn, uid=${firebaseAuth.currentUser?.uid}")
            authenticated = isSignedIn
            if (isSignedIn) signInError = null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signInLauncher = rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
        if (result.resultCode == Activity.RESULT_OK || auth.currentUser != null) {
            authenticated = true
            signInError = null
        } else {
            val error = result.idpResponse?.error
            signInError = if (error != null) {
                val cause = error.cause?.message
                "Error ${error.errorCode}: ${error.message ?: "(no message)"}${if (cause != null) "\nCause: $cause" else ""}"
            } else {
                "Sign in cancelled."
            }
        }
    }

    fun launchSignIn() {
        signInError = null
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.mipmap.ic_launcher)
            .setAvailableProviders(listOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
            ))
            .build()
        signInLauncher.launch(intent)
    }

    LaunchedEffect(authenticated) {
        if (!authenticated && signInError == null) {
            launchSignIn()
        }
    }

    when {
        authenticated -> content {
            AuthUI.getInstance().signOut(context).addOnCompleteListener {
                authenticated = false
                signInError = null
            }
        }
        signInError != null -> SignInFailedScreen(message = signInError!!, onRetry = { launchSignIn() })
    }
}

@Composable
private fun SignInFailedScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sign in failed.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try again")
        }
    }
}

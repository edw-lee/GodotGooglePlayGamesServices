package com.crustywalls.godotgoogleplaygamesservices

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin

class SignInController(
    private val godot: Godot,
    private val root: GodotGooglePlayGamesServices
) {
    private val activity = godot.activity as Activity;
    val signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN;

    companion object {
        const val RC_SIGN_IN = 77;
    }

    fun signIn() {
        if (isConnected().first) {
            root.onSignInSuccess();
        } else {
            val signInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
            signInClient
                .silentSignIn()
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        root.onSignInSuccess();
                    } else {
                        interactiveSignIn(signInClient);
                    }
                }
        }
    }

    private fun interactiveSignIn(signInClient: GoogleSignInClient) {
        val intent: Intent = signInClient.signInIntent;
        activity.startActivityForResult(intent, RC_SIGN_IN);
    }

    fun onSignInActivityResult(googleSignInResult: GoogleSignInResult?) {
        if (googleSignInResult != null && googleSignInResult.isSuccess) {
            root.onSignInSuccess();
        } else {
            var errorCode = Int.MIN_VALUE;
            googleSignInResult?.status?.let {
                errorCode = it.statusCode;
            };

            root.onSignInFailed(errorCode);
        }
    }

    fun signOut(signInClient:GoogleSignInClient) {
        signInClient.signOut()
            .addOnCompleteListener(activity) { task ->
                if(task.isSuccessful) {
                    root.onSignOutSuccess();
                } else {
                    root.onSignOutFailed();
                }
            };
    }

    fun isSignedIn():Boolean {
        val googleSignedInAcct = GoogleSignIn.getLastSignedInAccount(activity);
        return isConnected().first && googleSignedInAcct != null;
    }

    fun isConnected():Pair<Boolean, String> {
        val googleSignedIdAcct = GoogleSignIn.getLastSignedInAccount(activity);
        var accountId = "";

        googleSignedIdAcct?.id?.let {
            accountId = it;
        }

        return Pair(GoogleSignIn.hasPermissions(googleSignedIdAcct, *signInOptions.scopeArray), accountId);
    }
}
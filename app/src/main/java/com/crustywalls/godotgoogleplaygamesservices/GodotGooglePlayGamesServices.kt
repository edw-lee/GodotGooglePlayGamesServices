package com.crustywalls.godotgoogleplaygamesservices

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.games.Games
import org.godotengine.godot.Godot
import  org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo

class GodotGooglePlayGamesServices(godot: Godot) : GodotPlugin(godot) {

    private lateinit var signInController: SignInController;
    private lateinit var googleSignInClient: GoogleSignInClient;
    private lateinit var godotActivity: Activity;

    private var isEnablePlayGamesPopup = false;

    companion object {
        val SIGNAL_SIGNIN_SUCCESS = SignalInfo("sign_in_success");
        val SIGNAL_SIGNIN_FAILED = SignalInfo("sign_in_failed", Int::class.javaObjectType);
        val SIGNAL_SIGNOUT_SUCCESS = SignalInfo("sign_out_success");
        val SIGNAL_SIGNOUT_FAILED = SignalInfo("sign_out_failed");
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onMainActivityResult(requestCode, resultCode, data)

        if (requestCode == SignInController.RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data as Intent);
            signInController.onSignInActivityResult(result);
        }
    }

    override fun getPluginMethods(): MutableList<String> {
        return mutableListOf(
            "initialize",
            "signIn",
            "signOut",
            "isSignedIn"
        );
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SIGNAL_SIGNIN_SUCCESS,
            SIGNAL_SIGNIN_FAILED,
            SIGNAL_SIGNOUT_SUCCESS,
            SIGNAL_SIGNOUT_FAILED
        );
    }

    override fun getPluginName(): String {
        return BuildConfig.LIBRARY_NAME
    }

    fun initialize(enablePlayGamesPopup: Boolean) {
        godotActivity = godot.activity as Activity;

        signInController = SignInController(godot, this);

        googleSignInClient = GoogleSignIn.getClient(godotActivity, signInController.signInOptions)

        this.isEnablePlayGamesPopup = enablePlayGamesPopup;
    }

    private fun enablePlayGamesPopups() {
        val lastSignedInClient = GoogleSignIn.getLastSignedInAccount(godotActivity);

        if (lastSignedInClient != null) {
            Games.getGamesClient(godotActivity, lastSignedInClient)
                .setViewForPopups(godotActivity.findViewById(android.R.id.content));
        }
    }

    fun signIn() {
        runOnUiThread {
            signInController.signIn();
        }
    }

    fun signOut() {
        runOnUiThread {
            signInController.signOut(googleSignInClient);
        }
    }

    fun isSignedIn(): Boolean {
        return signInController.isSignedIn();
    }

    fun onSignInSuccess() {
        if (isEnablePlayGamesPopup)
            enablePlayGamesPopups();

        emitSignal(SIGNAL_SIGNIN_SUCCESS.name);
    }

    fun onSignInFailed(errorCode: Int) {
        emitSignal(SIGNAL_SIGNIN_FAILED.name, errorCode);
    }

    fun onSignOutSuccess() {
        emitSignal(SIGNAL_SIGNOUT_SUCCESS.name);
    }

    fun onSignOutFailed() {
        emitSignal(SIGNAL_SIGNOUT_FAILED.name);
    }
}
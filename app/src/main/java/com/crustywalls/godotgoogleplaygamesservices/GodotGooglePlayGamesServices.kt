package com.crustywalls.godotgoogleplaygamesservices

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataEntity
import org.godotengine.godot.Godot
import  org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import java.math.BigInteger
import java.util.*

class GodotGooglePlayGamesServices(godot: Godot) : GodotPlugin(godot) {

    private lateinit var signInController: SignInController;
    private lateinit var leaderboardController: LeaderboardController;
    private lateinit var savedGamesController: SavedGamesController;
    private lateinit var googleSignInClient: GoogleSignInClient;
    private lateinit var godotActivity: Activity;

    private lateinit var saveGameName:String;

    private var isEnablePlayGamesPopup = false;

    private var leaderboardID = "";

    companion object {
        val SIGNAL_SIGNIN_SUCCESS = SignalInfo("sign_in_success");
        val SIGNAL_SIGNIN_FAILED = SignalInfo("sign_in_failed", Int::class.javaObjectType);
        val SIGNAL_SIGNOUT_SUCCESS = SignalInfo("sign_out_success");
        val SIGNAL_SIGNOUT_FAILED = SignalInfo("sign_out_failed");
        val SIGNAL_LEADERBOARD_SCORE_SUBMITTED = SignalInfo("leaderboard_score_submitted");
        val SIGNAL_LEADERBOARD_SCORE_SUBMIT_FAILED = SignalInfo("leaderboard_score_submit_failed");
        val SIGNAL_LEADERBOARD_SCORE_RETRIEVED = SignalInfo("leaderboard_score_retrieved", Int::class.javaObjectType);
        val SIGNAL_LEADERBOARD_TOPSCORES_RERIEVED = SignalInfo("leaderboard_topscores_retrieved", String::class.java);
        val SIGNAL_SAVED_GAMES_SUCCESS = SignalInfo("on_saved_games_success");
        val SIGNAL_SAVED_GAMES_FAILED = SignalInfo("on_saved_games_failed");
        val SIGNAL_SAVED_GAMES_LOAD_SUCCESS = SignalInfo("on_saved_games_load_success", String::class.java);
        val SIGNAL_SAVED_GAMES_LOAD_FAILED = SignalInfo("on_saved_games_load_failed");
        val SIGNAL_SAVED_GAMES_CREATE_SNAPSHOT = SignalInfo("on_saved_games_snapshot_created", String::class.java);
    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onMainActivityResult(requestCode, resultCode, data)

        if (requestCode == SignInController.RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data as Intent);
            signInController.onSignInActivityResult(result);
        } else if (requestCode == SavedGamesController.RC_SAVED_GAMES) {
            if(data != null) {
                if(data.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                    data.getParcelableExtra<SnapshotMetadata>(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)?.let {
                        savedGamesController.loadSnapshot(it.uniqueName);
                    }
                } else if (data.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                    val unique = BigInteger(281, Random()).toString(13);
                    onSavedGamesCreateSnapshot("$saveGameName$unique")
                }
            }
        }
    }

    override fun getPluginMethods(): MutableList<String> {
        return mutableListOf(
            "initialize",
            "signIn",
            "signOut",
            "isSignedIn",
            "showLeaderboard",
            "showAllLeaderboards",
            "submitLeaderboardScore",
            "getLeaderboardCurrentPlayerScore",
            "getLeaderboardTopScores",
            "showSavedGames",
            "saveSnapshot",
            "loadSnapshot"
        );
    }

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            SIGNAL_SIGNIN_SUCCESS,
            SIGNAL_SIGNIN_FAILED,
            SIGNAL_SIGNOUT_SUCCESS,
            SIGNAL_SIGNOUT_FAILED,
            SIGNAL_LEADERBOARD_SCORE_SUBMITTED,
            SIGNAL_LEADERBOARD_SCORE_SUBMIT_FAILED,
            SIGNAL_LEADERBOARD_SCORE_RETRIEVED,
            SIGNAL_LEADERBOARD_TOPSCORES_RERIEVED,
            SIGNAL_SAVED_GAMES_SUCCESS,
            SIGNAL_SAVED_GAMES_FAILED,
            SIGNAL_SAVED_GAMES_LOAD_SUCCESS,
            SIGNAL_SAVED_GAMES_LOAD_FAILED,
            SIGNAL_SAVED_GAMES_CREATE_SNAPSHOT
        );
    }

    override fun getPluginName(): String {
        return BuildConfig.LIBRARY_NAME
    }

    fun initialize(enablePlayGamesPopup: Boolean, saveGameName:String) {
        this.saveGameName = saveGameName;

        godotActivity = godot.activity as Activity;

        signInController = SignInController(godot, this);
        leaderboardController = LeaderboardController(godot, this, signInController);

        googleSignInClient = GoogleSignIn.getClient(godotActivity, signInController.signInOptions)

        isEnablePlayGamesPopup = enablePlayGamesPopup;

    }

    private fun enablePlayGamesPopups() {
        val lastSignedInClient = GoogleSignIn.getLastSignedInAccount(godotActivity);

        if (lastSignedInClient != null) {
            Games.getGamesClient(godotActivity, lastSignedInClient)
                .setViewForPopups(godotActivity.findViewById(android.R.id.content));
        }
    }

    //Sign In

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

    //Leaderboard

    fun showLeaderboard() {
        runOnUiThread {
            leaderboardController.showLeaderboard();
        }
    }

    fun showAllLeaderboards() {
        runOnUiThread {
            leaderboardController.showAllLeaderboards();
        }
    }

    fun submitLeaderboardScore(score:Int) {
        runOnUiThread {
            leaderboardController.submitScore(score);
        }
    }

    fun getLeaderboardCurrentPlayerScore() {
        runOnUiThread {
            leaderboardController.getCurrentPlayerScore();
        }
    }

    fun getLeaderboardTopScores(maxResults:Int) {
        runOnUiThread {
            leaderboardController.getTopScores(maxResults);
        }
    }

    //Saved Games

    fun showSavedGames(title: String, showAddBtn:Boolean, showDeleteBtn:Boolean, maxNumberOfSavedGamesToShow:Int) {
        runOnUiThread {
            savedGamesController.showSavedGamesUI(title, showAddBtn, showDeleteBtn, maxNumberOfSavedGamesToShow);
        }
    }

    fun saveSnapshot(name:String, data:String, description:String) {
        runOnUiThread {
            savedGamesController.saveSnapshot(name, data, description);
        }
    }

    fun loadSnapshot(name:String) {
        runOnUiThread {
            savedGamesController.loadSnapshot(name);
        }
    }

    //Signals

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

    fun onLeaderboardScoreSubmitted() {
        emitSignal(SIGNAL_LEADERBOARD_SCORE_SUBMITTED.name);
    }

    fun onLeaderboardScoreSubmitFailed() {
        emitSignal(SIGNAL_LEADERBOARD_SCORE_SUBMIT_FAILED.name);
    }

    fun onLeaderboardScoreRetrieved(score:Int) {
        emitSignal(SIGNAL_LEADERBOARD_SCORE_RETRIEVED.name, score);
    }

    fun onLeaderboardTopScoresRetrieved(topScoresJson:String) {
        emitSignal(SIGNAL_LEADERBOARD_TOPSCORES_RERIEVED.name, topScoresJson);
    }

    fun onSavedGamesSuccess() {
        emitSignal(SIGNAL_SAVED_GAMES_SUCCESS.name);
    }

    fun onSavedGamesFailed() {
        emitSignal(SIGNAL_SAVED_GAMES_FAILED.name);
    }

    fun onSavedGamesLoadSuccess(data:String) {
        emitSignal(SIGNAL_SAVED_GAMES_LOAD_SUCCESS.name, data);
    }

    fun onSavedGamesLoadFailed() {
        emitSignal(SIGNAL_SAVED_GAMES_LOAD_FAILED.name);
    }

    fun onSavedGamesCreateSnapshot(saveName:String){
        emitSignal(SIGNAL_SAVED_GAMES_CREATE_SNAPSHOT.name, saveName);
    }
}
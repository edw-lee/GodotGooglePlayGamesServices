package com.crustywalls.godotgoogleplaygamesservices

import android.app.Activity
import android.app.GameManager
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.LeaderboardVariant
import com.google.gson.Gson
import org.godotengine.godot.Godot

class LeaderboardController(
    private val godot: Godot,
    private val root: GodotGooglePlayGamesServices,
    private val signInController: SignInController,
) {
    data class TopScore(
        val Name: String,
        val Score: Long,
        val Rank: Long
    )

    private val activity = godot.activity as Activity;
    private val leaderboardID = godot.getString(R.string.leaderboard_id);

    companion object {
        const val RC_LEADERBOARD_UI = 9004;
    }

    fun submitScore(score: Int) {
        val googleSignInAcct = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInAcct != null) {
            val leaderboardsClient = Games.getLeaderboardsClient(activity, googleSignInAcct);

            leaderboardsClient.submitScoreImmediate(leaderboardID, score.toLong())
                .addOnSuccessListener { result -> root.onLeaderboardScoreSubmitted() }
                .addOnFailureListener { result -> root.onLeaderboardScoreSubmitFailed() }
        } else
            root.onLeaderboardScoreSubmitFailed();
    }

    fun showLeaderboard() {
        val googleSignInClient = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInClient != null) {
            val leaderboardsClient = Games.getLeaderboardsClient(activity, googleSignInClient);

            leaderboardsClient.getLeaderboardIntent(leaderboardID)
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(
                        intent,
                        RC_LEADERBOARD_UI
                    )
                }
        }
    }

    fun showAllLeaderboards() {
        val googleSignInClient = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInClient != null) {
            val leaderboardClient = Games.getLeaderboardsClient(activity, googleSignInClient);

            leaderboardClient.allLeaderboardsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(
                        intent,
                        RC_LEADERBOARD_UI
                    )
                }
        }
    }

    fun getCurrentPlayerScore() {
        val googleSignInClient = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInClient != null) {
            val leaderboardClient = Games.getLeaderboardsClient(activity, googleSignInClient);

            leaderboardClient.loadCurrentPlayerLeaderboardScore(
                leaderboardID,
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC
            )
                .addOnSuccessListener { result -> root.onLeaderboardScoreRetrieved(result.get()?.rawScore!!.toInt()) };
        }
    }

    fun getTopScores(maxResults: Int) {
        val googleSignInClient = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInClient != null) {
            val leaderboardsClient = Games.getLeaderboardsClient(activity, googleSignInClient);

            leaderboardsClient.loadTopScores(
                leaderboardID,
                LeaderboardVariant.TIME_SPAN_ALL_TIME,
                LeaderboardVariant.COLLECTION_PUBLIC,
                maxResults,
                false
            ).addOnSuccessListener { result ->
                var topScores:MutableList<TopScore> = ArrayList();

                result.get()?.scores?.forEach { score ->
                    topScores.add(TopScore(score.scoreHolderDisplayName, score.rawScore, score.rank));
                }

                root.onLeaderboardTopScoresRetrieved(Gson().toJson(topScores))
            }
        }
    }
}
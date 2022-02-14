package com.crustywalls.godotgoogleplaygamesservices

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.games.snapshot.Snapshots
import com.google.android.gms.tasks.Continuation
import org.godotengine.godot.Godot
import java.io.IOException

class SavedGamesController(
    private val godot: Godot,
    private val root: GodotGooglePlayGamesServices,
    private val signInController: SignInController
) {
    private val activity = godot.activity as Activity;

    companion object {
        const val RC_SAVED_GAMES = 9009;
    }

    fun showSavedGamesUI(
        title: String,
        showAddBtn: Boolean,
        showDeleteBtn: Boolean,
        maxNumberOfSavedGamesToShow: Int
    ) {
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity);

        if (signInController.isConnected().first && googleSignInAccount != null) {
            val snapshotClient = Games.getSnapshotsClient(activity, googleSignInAccount);
            val intentTask = snapshotClient.getSelectSnapshotIntent(
                title,
                showAddBtn,
                showDeleteBtn,
                maxNumberOfSavedGamesToShow
            )
        }
    }

    fun saveSnapshot(gameName: String, data: String, description: String) {
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity);
        if (signInController.isConnected().first && googleSignInAccount != null) {
            val snapshotsClient = Games.getSnapshotsClient(activity, googleSignInAccount);
            val conflictResolutionPolicy = Snapshots.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

            snapshotsClient.open(gameName, true, conflictResolutionPolicy)
                .addOnFailureListener {
                    root.onSavedGamesFailed();
                }
                .continueWith<Pair<Snapshot, ByteArray>>(
                    Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Pair<Snapshot, ByteArray>> { task ->
                        val snapshot = task.result;

                        snapshot?.data?.let {
                            return@Continuation Pair(it, toByteArray(data));
                        }
                    }
                )
                .addOnCompleteListener{ task ->
                    if(task.isSuccessful && task.result != null) {
                        val snapshot = task.result!!.first;
                        val dataBytes = task.result!!.second;

                        commitSnapshot(snapshot, dataBytes, description);
                    } else {
                        root.onSavedGamesFailed();
                    }
                }
        } else {
            root.onSavedGamesFailed();
        }
    }

    private fun commitSnapshot(snapshot: Snapshot, data:ByteArray, description: String) {
        snapshot.snapshotContents.writeBytes(data);

        val metadataChange = SnapshotMetadataChange.Builder()
            .setDescription(description)
            .build();

        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity);

        if(signInController.isConnected().first && googleSignInAccount != null) {
            val snapshotClient = Games.getSnapshotsClient(activity, googleSignInAccount);
            val commitTask = snapshotClient.commitAndClose(snapshot, metadataChange);
            commitTask.addOnCompleteListener { task ->
                if(task.isSuccessful)
                    root.onSavedGamesSuccess();
                else
                    root.onSavedGamesFailed();
            }
        } else {
            root.onSavedGamesFailed();
        }
    }

    fun loadSnapshot(gameName: String) {
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(activity);

        if(signInController.isConnected().first && googleSignInAccount != null) {
            val snapshotClient = Games.getSnapshotsClient(activity, googleSignInAccount);
            val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

            snapshotClient.open(gameName, true, conflictResolutionPolicy)
                .addOnFailureListener{
                    root.onSavedGamesLoadFailed();
                }
                .continueWith<ByteArray>(Continuation { task ->
                    val snapshot = task.result;
                    try {
                        snapshot?.data?.let {
                            return@Continuation it.snapshotContents.readFully();
                        }

                        return@Continuation null;
                    } catch (e: IOException) {
                        Log.e("godot", "Error while reading Snapshot.", e);
                    }
                    null;
                })
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        task.result?.let {
                            val data = toStringData(it);
                            root.onSavedGamesLoadSuccess(data);
                        }
                    } else {
                        root.onSavedGamesLoadFailed();
                    }
                }
        } else {
            root.onSavedGamesLoadFailed();
        }
    }

    private fun toByteArray(data: String): ByteArray {
        return data.toByteArray();
    }

    private fun toStringData(bytes: ByteArray): String {
        return String(bytes);
    }
}
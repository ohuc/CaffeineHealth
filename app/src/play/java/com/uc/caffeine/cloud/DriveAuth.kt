package com.uc.caffeine.cloud

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Why a Drive authorization did not produce a token. */
internal enum class DriveAuthFailure { CANCELLED, NEEDS_USER, UNAVAILABLE }

internal sealed interface DriveAuthOutcome {
    data class Granted(val accessToken: String) : DriveAuthOutcome
    data class Failed(val reason: DriveAuthFailure) : DriveAuthOutcome
}

private val driveRequest: AuthorizationRequest
    get() = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DriveBackupClient.SCOPE))).build()

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

/**
 * Background token for the worker: succeeds only if the user already granted Drive access.
 * Google refreshes the token itself; a revoked grant reports [DriveAuthFailure.NEEDS_USER].
 */
internal suspend fun silentDriveToken(context: Context): DriveAuthOutcome = try {
    val result = Identity.getAuthorizationClient(context).authorize(driveRequest).await()
    val token = result.accessToken
    when {
        result.hasResolution() -> DriveAuthOutcome.Failed(DriveAuthFailure.NEEDS_USER)
        token.isNullOrBlank() -> DriveAuthOutcome.Failed(DriveAuthFailure.UNAVAILABLE)
        else -> DriveAuthOutcome.Granted(token)
    }
} catch (_: Exception) {
    DriveAuthOutcome.Failed(DriveAuthFailure.UNAVAILABLE)
}

/** Asks for Drive access with Google's own consent screen when needed. */
internal class DriveAuthorizer(private val context: Context) {
    /** Set on every composition to the current activity-result launcher. */
    internal var launch: (IntentSenderRequest) -> Unit = {}
    private var pending: CompletableDeferred<DriveAuthOutcome>? = null

    suspend fun authorize(): DriveAuthOutcome {
        val result: AuthorizationResult = try {
            Identity.getAuthorizationClient(context).authorize(driveRequest).await()
        } catch (_: Exception) {
            return DriveAuthOutcome.Failed(DriveAuthFailure.UNAVAILABLE)
        }
        val intent = result.pendingIntent
        if (!result.hasResolution() || intent == null) {
            return result.accessToken?.takeIf { it.isNotBlank() }?.let { DriveAuthOutcome.Granted(it) }
                ?: DriveAuthOutcome.Failed(DriveAuthFailure.UNAVAILABLE)
        }
        val deferred = CompletableDeferred<DriveAuthOutcome>()
        pending = deferred
        launch(IntentSenderRequest.Builder(intent.intentSender).build())
        return deferred.await()
    }

    internal fun onResult(resultCode: Int, data: android.content.Intent?) {
        val deferred = pending ?: return
        pending = null
        if (resultCode != Activity.RESULT_OK) {
            deferred.complete(DriveAuthOutcome.Failed(DriveAuthFailure.CANCELLED))
            return
        }
        val token = runCatching {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
        }.getOrNull()
        deferred.complete(token?.takeIf { it.isNotBlank() }?.let { DriveAuthOutcome.Granted(it) }
            ?: DriveAuthOutcome.Failed(DriveAuthFailure.UNAVAILABLE))
    }
}

@Composable
internal fun rememberDriveAuthorizer(): DriveAuthorizer {
    val context = LocalContext.current
    val authorizer = remember(context) { DriveAuthorizer(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        authorizer.onResult(result.resultCode, result.data)
    }
    authorizer.launch = { launcher.launch(it) }
    return authorizer
}

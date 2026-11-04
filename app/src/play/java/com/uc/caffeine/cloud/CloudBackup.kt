package com.uc.caffeine.cloud

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.uc.caffeine.data.BackupManager
import com.uc.caffeine.data.CaffeineDatabase
import com.uc.caffeine.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** What stopped the last automatic backup, if anything. */
internal enum class CloudBackupProblem { SIGN_IN_NEEDED, FAILED }

internal data class CloudBackupState(
    val enabled: Boolean = false,
    val account: String? = null,
    val lastBackupMillis: Long? = null,
    val running: Boolean = false,
    val problem: CloudBackupProblem? = null,
)

/**
 * Google Drive backup of the same JSON the manual export writes, into the user's own hidden
 * app-data folder. Free for every Play user. The on/off choice lives in the edition's private
 * preferences (excluded from OS backup and from the portable JSON), never in [com.uc.caffeine.data.UserSettings].
 */
internal object CloudBackup {
    private const val PREFS = "cloud_backup"
    private const val PERIODIC = "cloud-backup-daily"
    private val mutex = Mutex()
    private val state = MutableStateFlow(CloudBackupState())
    @Volatile private var loaded = false

    fun state(context: Context): StateFlow<CloudBackupState> {
        load(context)
        return state.asStateFlow()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized private fun load(context: Context) {
        if (loaded) return
        val p = prefs(context)
        state.value = CloudBackupState(
            enabled = p.getBoolean("enabled", false),
            account = p.getString("account", null),
            lastBackupMillis = p.getLong("last", 0L).takeIf { it > 0L },
            problem = p.getString("problem", null)?.let { runCatching { CloudBackupProblem.valueOf(it) }.getOrNull() },
        )
        loaded = true
    }

    private fun save(context: Context, update: (CloudBackupState) -> CloudBackupState) {
        load(context)
        val next = update(state.value)
        state.value = next
        prefs(context).edit()
            .putBoolean("enabled", next.enabled)
            .putString("account", next.account)
            .putLong("last", next.lastBackupMillis ?: 0L)
            .putString("problem", next.problem?.name)
            .apply()
    }

    /** Work that must finish even if the screen that started it goes away (setup ends on restore). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Turns automatic backups on after the user granted Drive access. [uploadNow] is false right after
     * a restore, when Drive already holds this data.
     */
    fun enable(context: Context, accessToken: String, uploadNow: Boolean): Job = appScope.launch {
        val app = context.applicationContext
        val email = withContext(Dispatchers.IO) { DriveBackupClient(accessToken).accountEmail() }
        save(app) { it.copy(enabled = true, account = email ?: it.account, problem = null) }
        schedule(app)
        if (uploadNow) backUpNow(app, accessToken)
    }

    /** The newest backup already in this account's Drive (null when none), without downloading it. */
    suspend fun latestInDrive(accessToken: String): DriveBackupFile? =
        withContext(Dispatchers.IO) { DriveBackupClient(accessToken).latestBackup() }

    fun disable(context: Context) {
        save(context) { it.copy(enabled = false, problem = null, running = false) }
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }

    /** Daily, on any network, not on a low battery. Survives reboots (WorkManager). */
    private fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(PERIODIC)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Uploads the current data now. Returns true when the backup is in Drive. */
    suspend fun backUpNow(context: Context, accessToken: String): Boolean = mutex.withLock {
        save(context) { it.copy(running = true) }
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                val database = CaffeineDatabase.getDatabase(context)
                val settings = SettingsRepository(context)
                val json = BackupManager(database.consumptionLogDao(), database.drinkPresetDao(),
                    database.drinkUnitDao(), settings).createBackup(settings.settingsFlow.first())
                DriveBackupClient(accessToken).upload(json)
            }
        }
        val failure = outcome.exceptionOrNull()
        save(context) { current ->
            current.copy(
                running = false,
                lastBackupMillis = outcome.getOrNull()?.let { it.modifiedAtMillis ?: System.currentTimeMillis() }
                    ?: current.lastBackupMillis,
                problem = when {
                    failure == null -> null
                    failure is DriveHttpException && failure.authProblem -> CloudBackupProblem.SIGN_IN_NEEDED
                    else -> CloudBackupProblem.FAILED
                },
            )
        }
        failure == null
    }

    /** The newest backup in this account's Drive, downloaded, or null when there is none. */
    suspend fun downloadLatest(accessToken: String): String? = withContext(Dispatchers.IO) {
        val client = DriveBackupClient(accessToken)
        client.latestBackup()?.let(client::download)
    }

    suspend fun download(accessToken: String, file: DriveBackupFile): String =
        withContext(Dispatchers.IO) { DriveBackupClient(accessToken).download(file) }

    /** Removes every backup this app stored in the account's Drive and stops automatic backups. */
    suspend fun deleteFromDrive(context: Context, accessToken: String) {
        withContext(Dispatchers.IO) { DriveBackupClient(accessToken).deleteAll() }
        disable(context)
        save(context) { it.copy(lastBackupMillis = null) }
    }

    internal fun markSignInNeeded(context: Context) = save(context) { it.copy(problem = CloudBackupProblem.SIGN_IN_NEEDED, running = false) }
}

/** The daily (and one-off) automatic backup. Never prompts: without a grant it records that sign-in is needed. */
internal class CloudBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!CloudBackup.state(context).value.enabled) return Result.success()
        return when (val auth = silentDriveToken(context)) {
            is DriveAuthOutcome.Granted ->
                if (CloudBackup.backUpNow(context, auth.accessToken)) Result.success()
                else if (CloudBackup.state(context).value.problem == CloudBackupProblem.SIGN_IN_NEEDED) Result.success()
                else Result.retry()
            is DriveAuthOutcome.Failed -> when (auth.reason) {
                DriveAuthFailure.NEEDS_USER -> { CloudBackup.markSignInNeeded(context); Result.success() }
                else -> Result.retry()
            }
        }
    }
}

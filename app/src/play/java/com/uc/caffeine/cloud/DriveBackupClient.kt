package com.uc.caffeine.cloud

import com.uc.caffeine.util.readBackupJson
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

/** One backup file in the user's hidden Drive app-data folder. */
internal data class DriveBackupFile(val id: String, val modifiedAtMillis: Long?)

/** Drive answered with an HTTP error. 401/403 mean the grant is gone and the user must sign in again. */
internal class DriveHttpException(val code: Int) : IOException("Google Drive returned HTTP $code") {
    val authProblem: Boolean get() = code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN
}

/**
 * Minimal Google Drive v3 client for one file in the `appDataFolder` space: a folder only this app
 * can see, inside the user's own Drive, granted by the non-sensitive `drive.appdata` scope.
 * The developer has no copy and no server; the access token comes from Google's authorization API.
 *
 * Uploads never overwrite in place: a new file is written first and older ones are deleted only
 * after it succeeds, so a failed upload never leaves the user without a backup.
 */
internal class DriveBackupClient(private val accessToken: String) {

    fun latestBackup(): DriveBackupFile? = listBackups().firstOrNull()

    fun listBackups(): List<DriveBackupFile> {
        val query = URLEncoder.encode("name = '$FILE_NAME' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$query&orderBy=modifiedTime%20desc" +
            "&pageSize=20&fields=files(id,modifiedTime)"
        return parseFileList(request("GET", url))
    }

    /** Uploads [json] as the newest backup, then removes the older copies. Returns the new file. */
    fun upload(json: String): DriveBackupFile {
        val previous = listBackups()
        val boundary = "caffeine-${UUID.randomUUID()}"
        val created = JSONObject(request("POST", "$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime",
            body = multipartBody(boundary, json), contentType = "multipart/related; boundary=$boundary"))
        previous.forEach { old -> runCatching { delete(old.id) } }
        return DriveBackupFile(created.getString("id"), created.optString("modifiedTime").toMillisOrNull())
    }

    /** Downloads a backup, bounded like a file import (size limit, strict UTF-8, JSON object). */
    fun download(file: DriveBackupFile): String {
        val connection = open("GET", "$API/files/${file.id}?alt=media")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw DriveHttpException(code)
            return connection.inputStream.use { readBackupJson(it) }
        } finally {
            connection.disconnect()
        }
    }

    fun deleteAll() = listBackups().forEach { delete(it.id) }

    /** The signed-in account's email for display, or null if Drive does not share it. */
    fun accountEmail(): String? = runCatching {
        JSONObject(request("GET", "$API/about?fields=user(emailAddress)"))
            .optJSONObject("user")?.optString("emailAddress")?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun delete(id: String) {
        request("DELETE", "$API/files/$id")
    }

    private fun open(method: String, url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

    private fun request(method: String, url: String, body: ByteArray? = null, contentType: String? = null): String {
        val connection = open(method, url)
        try {
            if (body != null) {
                connection.doOutput = true
                contentType?.let { connection.setRequestProperty("Content-Type", it) }
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code !in 200..299) throw DriveHttpException(code)
            return if (code == HttpURLConnection.HTTP_NO_CONTENT) ""
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val FILE_NAME = "caffeine-health-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val TIMEOUT_MS = 20_000

        /** Drive multipart upload: JSON metadata (name, app-data parent), then the backup itself. */
        fun multipartBody(boundary: String, json: String): ByteArray {
            val metadata = JSONObject().put("name", FILE_NAME).put("parents", JSONArray().put("appDataFolder"))
            return buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString()).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(json).append("\r\n")
                append("--").append(boundary).append("--\r\n")
            }.toByteArray(Charsets.UTF_8)
        }

        fun parseFileList(body: String): List<DriveBackupFile> {
            val files = JSONObject(body).optJSONArray("files") ?: return emptyList()
            return (0 until files.length()).map { index ->
                val file = files.getJSONObject(index)
                DriveBackupFile(file.getString("id"), file.optString("modifiedTime").toMillisOrNull())
            }.sortedByDescending { it.modifiedAtMillis ?: 0L }
        }

        private fun String.toMillisOrNull(): Long? = takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    }
}

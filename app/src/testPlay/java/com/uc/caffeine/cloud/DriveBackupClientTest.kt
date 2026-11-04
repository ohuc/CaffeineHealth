package com.uc.caffeine.cloud

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DriveBackupClientTest {
    @Test fun `multipart body puts metadata in the hidden app folder then the backup`() {
        val body = String(DriveBackupClient.multipartBody("b0undary", """{"version":1}"""), Charsets.UTF_8)
        val parts = body.split("--b0undary")
        assertEquals("", parts[0])
        val metadata = JSONObject(parts[1].substringAfter("\r\n\r\n").trim())
        assertEquals(DriveBackupClient.FILE_NAME, metadata.getString("name"))
        assertEquals("appDataFolder", metadata.getJSONArray("parents").getString(0))
        assertEquals("""{"version":1}""", parts[2].substringAfter("\r\n\r\n").trim())
        assertTrue(body.endsWith("--b0undary--\r\n"))
    }

    @Test fun `file list is newest first and tolerates missing times`() {
        val files = DriveBackupClient.parseFileList(
            """{"files":[
                {"id":"old","modifiedTime":"2026-10-01T08:00:00.000Z"},
                {"id":"new","modifiedTime":"2026-11-02T08:00:00.000Z"},
                {"id":"unknown"}
            ]}""",
        )
        assertEquals(listOf("new", "old", "unknown"), files.map { it.id })
        assertNull(files.last().modifiedAtMillis)
    }

    @Test fun `empty or missing list means no backup`() {
        assertTrue(DriveBackupClient.parseFileList("""{"files":[]}""").isEmpty())
        assertTrue(DriveBackupClient.parseFileList("{}").isEmpty())
    }

    @Test fun `only revoked or forbidden access asks the user to sign in again`() {
        assertTrue(DriveHttpException(401).authProblem)
        assertTrue(DriveHttpException(403).authProblem)
        assertFalse(DriveHttpException(500).authProblem)
        assertFalse(DriveHttpException(404).authProblem)
    }

    @Test fun `scope is the non-sensitive app data folder only`() {
        assertEquals("https://www.googleapis.com/auth/drive.appdata", DriveBackupClient.SCOPE)
    }
}

package com.uc.caffeine.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.IOException

class BackupInputTest {
    @Test fun smallJsonIsPreserved() {
        val json = "  {\"entries\":[],\"name\":\"कॉफी\"}"
        assertEquals(json, readBackupJson(ByteArrayInputStream(json.toByteArray()), 128))
    }

    @Test fun stopsReadingAnOversizedStream() {
        var reads = 0
        val stream = object : InputStream() {
            override fun read(): Int { reads++; return 'x'.code }
        }
        assertThrows(BackupTooLargeException::class.java) { readBackupJson(stream, 32) }
        assertEquals(33, reads)
    }

    @Test fun rejectsNonJsonFiles() {
        assertThrows(IOException::class.java) { readBackupJson("<html>oops</html>".byteInputStream(), 128) }
    }

    @Test fun rejectsInvalidUtf8() {
        assertThrows(IOException::class.java) {
            readBackupJson(ByteArrayInputStream(byteArrayOf(123, -61, 40, 125)), 128)
        }
    }

    @Test fun acceptsExactlyTheLimit() {
        assertEquals("{}", readBackupJson("{}".byteInputStream(), 2))
    }
}

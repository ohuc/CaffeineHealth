package com.uc.caffeine.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

class BackupTooLargeException : IOException("Backup exceeds the size limit")

fun readBackupJson(stream: InputStream, maxBytes: Int = MAX_BACKUP_BYTES): String {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
    val buffer = ByteArray(minOf(maxBytes + 1, 8192))
    var total = 0
    while (true) {
        val count = stream.read(buffer, 0, minOf(buffer.size, maxBytes - total + 1))
        if (count == -1) break
        total += count
        if (total > maxBytes) throw BackupTooLargeException()
        output.write(buffer, 0, count)
    }
    val text = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(output.toByteArray())).toString()
    if (!text.trimStart().startsWith('{')) throw IOException("Select a JSON backup")
    return text
}

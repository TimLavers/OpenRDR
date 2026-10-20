package io.rippledown.integration.proxy

import java.io.File
import java.io.RandomAccessFile

fun copyLogTail(source: File, destination: File, startOffset: Long) {
    destination.parentFile?.mkdirs()
    RandomAccessFile(source, "r").use { input ->
        // A rollover or truncation starts a new log; retain what is still available.
        input.seek(if (startOffset in 0..input.length()) startOffset else 0)
        destination.outputStream().use { output ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
        }
    }
}

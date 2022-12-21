package io.rippledown.kb.export.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * Copied from Baeldung:
 * https://www.baeldung.com/java-compress-and-uncompress
 */
class Zipper(val directoryToZip: File) {

    fun zip(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(byteArrayOutputStream)

        zipFile(directoryToZip, directoryToZip.name, zipOut)
        zipOut.close()
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(ZipEntry(fileName))
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry("$fileName/"))
                zipOut.closeEntry()
            }
            val children = fileToZip.listFiles() ?: return
            for (childFile in children) {
                zipFile(childFile, fileName + "/" + childFile.name, zipOut)
            }
            return
        }
        val fis = FileInputStream(fileToZip)
        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }
}
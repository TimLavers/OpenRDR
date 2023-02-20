package io.rippledown.kb.export.util

import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8

fun checkFileContents(file: File, expected: String) {
    val fileBytes = Files.readAllBytes(file.toPath())
    val fileContentsAsString = String(fileBytes, UTF_8)
    fileContentsAsString shouldBe expected
}
class UnzipperTest {

    @Test
    fun unzip() {
        val zipFile = File("src/jvmTest/resources/util/stuff.zip")
        val bytes = Files.readAllBytes(zipFile.toPath())
        val tempDir: File = createTempDirectory().toFile()
        tempDir.mkdirs()
        tempDir.listFiles()!!.size shouldBe 0

        Unzipper(bytes, tempDir).unzip()

        tempDir.listFiles()!!.size shouldBe 1
        val topDir = File(tempDir, "stuff")
        topDir.listFiles()!!.size shouldBe 3
        checkFileContents(File(topDir, "File1.txt"), "This is File1.")
        checkFileContents(File(topDir, "File2.txt"), "This is File2.")

        val d1 = File(topDir, "d1")
        d1.listFiles()!!.size shouldBe 2
        checkFileContents(File(d1, "File3.txt"), "This is File3.")

        val d2 = File(d1, "d2")
        d2.listFiles()!!.size shouldBe 1
        checkFileContents(File(d2, "File4.txt"), "This is File4.")
    }
}
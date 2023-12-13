package io.rippledown.kb.export.util

import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class ZipperTest {

    @Test
    fun zip() {
        val toZip = File("src/test/resources/util/to_zip")
        val zipped = Zipper(toZip).zip()

        // Use the unzipper to check the zip file.
        val tempDir: File = createTempDirectory().toFile()
        tempDir.mkdirs()
        Unzipper(zipped, tempDir).unzip()

        tempDir.listFiles()!!.size shouldBe 1
        val topDir = File(tempDir, "to_zip")
        topDir.listFiles()!!.size shouldBe 3
        checkFileContents(File(topDir, "English.txt"), "This is some English text.")
        checkFileContents(File(topDir, "Armenian.txt"), "Սա հայերեն տեքստ է։")

        val d1 = File(topDir, "directory")
        d1.listFiles()!!.size shouldBe 2
        checkFileContents(File(d1, "Chinese.txt"), "這是一些中文文本。")
        checkFileContents(File(d1, "Arabic.txt"), "هذا نص عربي.")
    }
}
package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.apache.commons.io.FileUtils
import org.junit.Before
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class FilenameMakerTest {
    private var tempDir: File = createTempDirectory().toFile()

    init {
        tempDir.mkdirs()
    }

    @Before
    fun init() {
        FileUtils.cleanDirectory(tempDir)
    }

    @Test
    fun `handle empty`() {
        FilenameMaker(emptySet()).makeUniqueNames() shouldBe emptyMap()
    }

    @Test
    fun createUniqueNames() {
        val map = FilenameMaker(setOf("cats", "dogs", "horses")).makeUniqueNames()
        map.size shouldBe 3
        map["cats"] shouldBe "cats.json"
        map["dogs"] shouldBe "dogs.json"
        map["horses"] shouldBe "horses.json"
        checkAreValidFilenames(map)
    }

    @Test
    fun useSuffix() {
        val map = FilenameMaker(setOf("cats", "dogs")).makeUniqueNames(".txt")
        map.size shouldBe 2
        map["cats"] shouldBe "cats.txt"
        map["dogs"] shouldBe "dogs.txt"
        checkAreValidFilenames(map)
    }

    @Test
    fun `handle illegal chars`() {
        val colon = "HDL:LDL good"
        val backslash = "TSH \\ FT3"
        val forwardSlash = "HDL/LDL great"
        val lt = "age > 80"
        val gt = "age < 80"
        val doubleQuote = "Ward is \"Maternity\""
        val pipe = "high|very high"
        val questionMark = "Euthyroid?"
        val asterisk = "FT3*"
        val dot = ".FT3"
        val names = mutableSetOf(colon, backslash, forwardSlash, lt, gt, doubleQuote, pipe, questionMark, asterisk, dot)
        val map = FilenameMaker(names).makeUniqueNames(".txt")
        map.size shouldBe  names.size
        map[colon] shouldBe "HDL_LDL good.txt"
        map[backslash] shouldBe "TSH _ FT3.txt"
        checkAreValidFilenames(map)
    }

    @Test
    fun `handle case sensitivity`() {
        val map = FilenameMaker(setOf(">>cat<<", ">>CAT<<")).makeUniqueNames()
        map.size shouldBe 2
        map[">>cat<<"] shouldStartWith  "__cat__.json"
        map[">>cat<<"] shouldEndWith  ".json"
        map[">>CAT<<"] shouldStartWith  "__CAT__"
        map[">>CAT<<"] shouldEndWith  ".json"
        checkAreValidFilenames(map)
    }

    @Test
    fun `handle strings that differ only by illegal chars`() {
        val map = FilenameMaker(setOf(">>cat<<", ">-cat-<")).makeUniqueNames()
        map.size shouldBe 2
        map[">>cat<<"] shouldStartWith  "__cat__.json"
        map[">>cat<<"] shouldEndWith  ".json"
        map[">-cat-<"] shouldStartWith  "_-cat-_"
        map[">-cat-<"] shouldEndWith  ".json"
        checkAreValidFilenames(map)
    }

    private fun checkAreValidFilenames(nameToFilename: Map<String, String>) {
        nameToFilename.values.forEach{ checkIsValidFilename(it)}
    }

    private fun checkIsValidFilename(name: String) {
        val textFile = File(tempDir, name)
        FileUtils.writeStringToFile(textFile, "Whatever", Charset.defaultCharset())
    }
}
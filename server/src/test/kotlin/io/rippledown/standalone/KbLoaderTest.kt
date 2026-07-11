package io.rippledown.standalone

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test

class KbLoaderTest: StandAloneInterpreterTestBase() {
    private lateinit var kbLoader: KbLoader
    private lateinit var tempDir: File
    @BeforeTest
    override fun setup() {
        super.setup()
        tempDir = createTempDirectory().toFile()
    }

    @Test
    fun `should load a kb`() {

    }
}
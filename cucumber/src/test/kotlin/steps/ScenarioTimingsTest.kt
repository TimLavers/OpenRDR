package steps

import io.kotest.matchers.shouldBe
import java.io.File
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class ScenarioTimingsTest {

    @Test
    fun `the folder is the directory under requirements`() {
        // Given
        val uri = URI("file:///C:/repos/OpenRDR/cucumber/src/test/resources/requirements/chat/Remove%20comment.feature")

        // When / Then
        featureFolderOf(uri) shouldBe "chat"
        featureNameOf(uri) shouldBe "Remove comment"
    }

    @Test
    fun `a feature outside requirements has an unknown folder`() {
        // Given
        val uri = URI("file:///C:/elsewhere/Some.feature")

        // When / Then
        featureFolderOf(uri) shouldBe "unknown"
    }

    @Test
    fun `fields containing commas or quotes are quoted`() {
        // Given
        val timing = ScenarioTiming("kb", "KB Management", "Export, then \"import\"", "PASSED", 12, 3)

        // When / Then
        timing.csvLine() shouldBe "\"kb\",\"KB Management\",\"Export, then \"\"import\"\"\",\"PASSED\",\"12\",\"3\",\"0.0\""
    }

    @Test
    fun `a new file gets the header and later timings append, across instances`() {
        // Given
        val dir = createTempDirectory().toFile()

        // When
        ScenarioTimingsFile(dir).append(ScenarioTiming("chat", "A", "one", "PASSED", 1, 0))
        ScenarioTimingsFile(dir).append(ScenarioTiming("chat", "A", "two", "FAILED", 2, 5))

        // Then
        File(dir, "chat.csv").readLines() shouldBe listOf(
            ScenarioTiming.CSV_HEADER,
            "\"chat\",\"A\",\"one\",\"PASSED\",\"1\",\"0\",\"0.0\"",
            "\"chat\",\"A\",\"two\",\"FAILED\",\"2\",\"5\",\"0.0\""
        )
    }
}

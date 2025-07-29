package io.rippledown.kb.sample

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB.*
import io.rippledown.server.KBEndpoint
import io.rippledown.util.EntityRetrieval
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SampleKBLoaderTest {
    private val kbName = "Whatever"
    private val persistenceProvider = InMemoryPersistenceProvider()
    private val kbManager = KBManager(persistenceProvider)

    private lateinit var endpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        val rootDir = File("kbe")
        val kbInfo = kbManager.createKB(kbName)
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        endpoint = KBEndpoint(kb, rootDir)
        FileUtils.cleanDirectory(endpoint.casesDir)
        FileUtils.cleanDirectory(endpoint.interpretationsDir)
    }

    @AfterTest
    fun cleanup() {
        kbManager.deleteKB(endpoint.kbInfo())
    }

    @Test
    fun `load TSH sample`() {
        loadSampleKB(endpoint, TSH)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 34
        caseNames[0] shouldBe "1.4.1"
        caseNames[1] shouldBe "1.4.2"
        caseNames[33] shouldBe "1.4.35"
        endpoint.kb.ruleTree.size() shouldBe 34
    }

    @Test
    fun `load TSH Cases sample`() {
        loadSampleKB(endpoint, TSH_CASES)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 34
        caseNames[0] shouldBe "1.4.1"
        caseNames[1] shouldBe "1.4.2"
        caseNames[33] shouldBe "1.4.35"
        endpoint.kb.ruleTree.size() shouldBe 1
    }

    @Test
    fun `load Contact Lenses sample`() {
        loadSampleKB(endpoint, CONTACT_LENSES)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 24
        caseNames[0] shouldBe "Case1"
        caseNames[1] shouldBe "Case2"
        caseNames[23] shouldBe "Case24"
        endpoint.kb.ruleTree.size() shouldBe 6
    }

    @Test
    fun `load Contact Lenses Cases sample`() {
        loadSampleKB(endpoint, CONTACT_LENSES_CASES)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 24
        caseNames[0] shouldBe "Case1"
        caseNames[1] shouldBe "Case2"
        caseNames[23] shouldBe "Case24"
        endpoint.kb.ruleTree.size() shouldBe 1L
    }


    @Test
    fun `load Zoo Animals sample`() {
        loadSampleKB(endpoint, ZOO)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 101
        caseNames[0] shouldBe "aardvark"
        caseNames[1] shouldBe "antelope"
        caseNames[2] shouldBe "bass"
        endpoint.kb.ruleTree.size() shouldBe 18
    }

    @Test
    fun `load Zoo Animals cases-only sample`() {
        loadSampleKB(endpoint, ZOO_CASES)
        val caseNames = endpoint.kb.processedCaseIds().map { it.name }
        caseNames shouldHaveSize 101
        caseNames[0] shouldBe "aardvark"
        caseNames[1] shouldBe "antelope"
        caseNames[2] shouldBe "bass"
        endpoint.kb.ruleTree.size() shouldBe 1
    }

    @Test
    fun `cannot load into a KB that already has rules`() {
        loadSampleKB(endpoint, TSH)
        shouldThrow<IllegalArgumentException> {
            loadSampleKB(endpoint, TSH)
        }.message shouldBe "Cannot load a sample into a KB that already has rules."
    }
}
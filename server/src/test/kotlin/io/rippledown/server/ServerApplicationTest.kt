package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import io.rippledown.constants.server.DEFAULT_PROJECT_NAME
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB.TSH
import io.rippledown.sample.SampleKB.TSH_CASES
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun RDRCase.getAttribute(attributeName: String): Attribute {
    return attributes.first { attribute -> attribute.name == attributeName }
}

fun RDRCase.getLatest(attributeName: String): TestResult? {
    return getLatest(getAttribute(attributeName))
}

internal class ServerApplicationTest {

    private val persistenceProvider = InMemoryPersistenceProvider()
    private lateinit var app: ServerApplication

    @BeforeTest
    fun setup() {
        app = ServerApplication(persistenceProvider)
        FileUtils.cleanDirectory(app.kbDataDir)
    }

    @Test
    fun casesDir() {
        assertEquals(app.kbDataDir, File("data"))
        assertTrue(app.kbDataDir.exists())
    }

    @Test
    fun interpretationsDir() {
        assertEquals(app.kbDataDir, File("data"))
        assertTrue(app.kbDataDir.exists())
    }

    @Test
    fun `get default project`() {
        app.kbList().size shouldBe 0
        persistenceProvider.idStore().data().keys shouldBe emptySet()
        val kbInfoDefault = app.getDefaultProject()

        app.kbList().size shouldBe 1
        app.kbList()[0] shouldBe kbInfoDefault
        kbInfoDefault.name shouldBe DEFAULT_PROJECT_NAME
        persistenceProvider.idStore().data().keys shouldBe setOf(kbInfoDefault.id)

        val kbInfo2 = app.getDefaultProject()
        kbInfo2.name shouldBe DEFAULT_PROJECT_NAME
        kbInfo2.id shouldBe kbInfoDefault.id
        app.kbList().size shouldBe 1
        app.kbList()[0] shouldBe kbInfoDefault
    }

    @Test // KBM-6
    fun `the KBs are loaded at init`() {
        val whatever = "Whatever"
        val kbInfoWhatever = app.createKB(whatever, false)
        val stuff = "Stuff"
        val kbInfoStuff = app.createKB(stuff, false)
        app.kbList() shouldBe listOf(kbInfoStuff, kbInfoWhatever) // Sanity check.

        app = ServerApplication(persistenceProvider)
        app.kbList() shouldBe listOf(kbInfoStuff, kbInfoWhatever) // KBs are loaded from the persistence provider.
        app.kbForId(kbInfoStuff.id).kb.kbInfo shouldBe kbInfoStuff
        app.kbForId(kbInfoWhatever.id).kb.kbInfo shouldBe kbInfoWhatever
    }

    @Test
    fun selectKB() {
        val kbi1 = app.createKB("KB1", false)
        val kbi2 = app.createKB("KB2", false)
        val kbi3 = app.createKB("KB3", false)
        app.selectKB(kbi1.id) shouldBe kbi1
        app.selectKB(kbi2.id) shouldBe kbi2
        app.selectKB(kbi3.id) shouldBe kbi3
    }

    @Test
    fun `should not create a KB with the same name as an existing KB if force is false`() {
        app.kbList().size shouldBe 0
        val kbName = "Whatever"
        app.createKB(kbName, false)

        //Given
        app.kbList().size shouldBe 1
        val kbInfo = app.kbList()[0]
        kbInfo.name shouldBe kbName
        val id0 = persistenceProvider.idStore().data().keys.first()
        app.kbFor(kbInfo).kb.addCornerstoneCase(createCase("Case1"))
        app.kbFor(kbInfo).kb.containsCornerstoneCaseWithName("Case1") shouldBe true

        //When
        try {
            app.createKB(kbName, false)
        } catch (e: Exception) {
            //expected
        }

        //Then
        app.kbFor(kbInfo).kb.kbInfo.name shouldBe kbName
        app.kbFor(kbInfo).kb.containsCornerstoneCaseWithName("Case1") shouldBe true //kb not rebuilt
        persistenceProvider.idStore().data().keys shouldBe setOf(id0)
    }

    @Test
    fun `should create a KB with the same name if force is true`() {
        app.kbList().size shouldBe 0
        val kbName = "Whatever"
        val kbi0 = app.createKB(kbName, false)

        //Given
        app.kbList().size shouldBe 1
        app.kbList()[0] shouldBe kbi0
        kbi0.name shouldBe kbName
        app.kbFor(kbi0).kb.addCornerstoneCase(createCase("Case1"))
        app.kbFor(kbi0).kb.containsCornerstoneCaseWithName("Case1") shouldBe true

        //When - Existing name, force true.
        val kbi1 = app.createKB(kbName, true)

        //Then
        with(app.kbList()) {
            size shouldBe 2
            this.shouldContain(kbi0)
            get(0).name shouldBe kbName
            get(1).name shouldBe kbName
        }

        app.kbFor(kbi0).kb.containsCornerstoneCaseWithName("Case1") shouldBe true //original kb
        app.kbFor(kbi1).kb.containsCornerstoneCaseWithName("Case1") shouldBe false //new kb with same name
        persistenceProvider.idStore().data().keys shouldBe setOf(kbi0.id, kbi1.id)
    }
/*
    @Test
    fun `should create a KB with a new name`() {
        //Given
        app.kbList().size shouldBe 0
        val kbName0 = "KB0"
        app.createKB(kbName0, false)
        val id0 = app.kbList()[0]

        //When
        app.createKB("KB1", false)
        val id0 = app.kbList()[0]
        app.createKB("KB2", true)
        val id3 = app.kb.kbInfo.id

        //Then - check that all the other KBs are still there.
        persistenceProvider.idStore().data().keys shouldBe setOf(id0, id1, id2, id3)
    }
   */
    @Test
    fun `handle zip in bad format`() {
        val zipFile = File("src/test/resources/export/NoRootDir.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun `handle empty zip`() {
        val zipFile = File("src/test/resources/export/Empty.zip").toPath()
        shouldThrow<IllegalArgumentException> {
            app.importKBFromZip(Files.readAllBytes(zipFile))
        }.message shouldBe "Invalid zip for KB import."
    }

    @Test
    fun kbList() {
        app.kbList().map { it.name } shouldBe listOf()
        app.createKB("Glucose", false)
        app.kbList().map { it.name } shouldBe listOf("Glucose",)
        app.createKB("Thyroids", true)
        app.kbList().map { it.name } shouldBe listOf("Glucose", "Thyroids")
        app.createKB("Thyroids", true)
        app.kbList().map { it.name } shouldBe listOf("Glucose", "Thyroids", "Thyroids")
        app.createKB("Whatever", true)
        app.kbList().map { it.name } shouldBe listOf("Glucose", "Thyroids", "Thyroids", "Whatever")
        app.createKB("Blah", true)
        app.kbList().map { it.name } shouldBe listOf("Blah", "Glucose", "Thyroids", "Thyroids", "Whatever")
    }

    @Test
    fun `create KB from sample`() {
        app.kbList().size shouldBe 0
        val kbName = "Whatever"
        val info = app.createKBFromSample(kbName, TSH)
        info.name shouldBe kbName
        app.kbList() shouldBe listOf(info)

        // Check the kb.
        app.kbFor(info).kb.allProcessedCases() shouldHaveSize 34
        app.kbFor(info).kb.ruleTree.size() shouldBe 34

        // Now create another.
        val kbName2 = "Stuff"
        val info2 = app.createKBFromSample(kbName2, TSH_CASES)
        info2.name shouldBe kbName2
        app.kbList() shouldBe listOf(info2, info)
        app.kbFor(info2).kb.allProcessedCases() shouldHaveSize 34
        app.kbFor(info2).kb.ruleTree.size() shouldBe 1
    }

    @Test
    fun `should not create a KB from sample with existing KB name`() {
        app.kbList().size shouldBe 0
        val kbName = "Whatever"
        app.createKB(kbName, false)

        //Given
        app.kbList().size shouldBe 1
        val kbInfo = app.kbList()[0]
        kbInfo.name shouldBe kbName
        val id0 = persistenceProvider.idStore().data().keys.first()

        //When
        try {
            app.createKBFromSample(kbName, TSH)
        } catch (e: Exception) {
            //expected
        }

        //Then
        app.kbFor(kbInfo).kb.kbInfo.name shouldBe kbName
        persistenceProvider.idStore().data().keys shouldBe setOf(id0)
    }

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}
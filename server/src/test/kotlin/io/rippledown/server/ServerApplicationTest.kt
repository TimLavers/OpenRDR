package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.CaseTestUtils
import io.rippledown.constants.server.DEFAULT_PROJECT_NAME
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.server.websocket.WebSocketManager
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Files
import kotlin.test.Test

fun RDRCase.getAttribute(attributeName: String): Attribute {
    return attributes.first { attribute -> attribute.name == attributeName }
}

fun RDRCase.getLatest(attributeName: String): TestResult? {
    return getLatest(getAttribute(attributeName))
}

internal class ServerApplicationTest {

    private lateinit var persistenceProvider: PersistenceProvider
    private lateinit var app: ServerApplication
    private lateinit var webSocketManager: WebSocketManager

    @BeforeEach
    fun setup() {
        persistenceProvider = InMemoryPersistenceProvider()
        webSocketManager = mockk()
        app = ServerApplication(persistenceProvider, webSocketManager)
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


    @Test
    fun `the KBs are loaded at init`() {
        //Given
        val whatever = "Whatever"
        val kbInfoWhatever = app.createKB(whatever, false)
        val stuff = "Stuff"

        //When
        val kbInfoStuff = app.createKB(stuff, false)

        //Then
        app.kbList() shouldBe listOf(kbInfoStuff, kbInfoWhatever) // Sanity check.
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
    fun `client feedback via websocket upon kb selection`() {
        val kbi1 = app.createKB("KB1", false)
        val kbi2 = app.createKB("KB2", false)
        app.selectKB(kbi1.id)
        coVerify { webSocketManager.sendKbInfo(kbi1) }
        app.selectKB(kbi2.id)
        coVerify { webSocketManager.sendKbInfo(kbi2) }
    }

    @Test
    fun `client feedback via websocket upon kb open by name`() {
        val kbi1 = app.createKB("KB1", false)
        app.createKB("KB2", false)

        val result1 = app.openKB(kbi1.name)
        result1.isSuccess shouldBe true
        result1.getOrNull()!! shouldBe kbi1
        coVerify { webSocketManager.sendKbInfo(kbi1) }
    }

    @Test
    fun kbForIdTest() {
        val kbi1 = app.createKB("KB1", false)
        val kbi2 = app.createKB("KB2", false)
        val kbi3 = app.createKB("KB3", false)
        app.kbForId(kbi1.id).kbInfo() shouldBe kbi1
        app.kbForId(kbi2.id).kbInfo() shouldBe kbi2
        app.kbForId(kbi3.id).kbInfo() shouldBe kbi3
    }

    @Test
    fun `unknown kb id`() {
        shouldThrow<IllegalArgumentException> {
            app.kbForId("Unknown")
        }.message shouldBe "Unknown kb id: Unknown"
    }

    @Test
    fun kbForName() {
        val kbi1 = app.createKB("KB1", false)
        val kbi2 = app.createKB("KB2", false)
        val kbi3 = app.createKB("KB3", false)
        app.kbForName(kbi1.name).getOrThrow().kbInfo() shouldBe kbi1
        app.kbForName(kbi2.name).getOrThrow().kbInfo() shouldBe kbi2
        app.kbForName(kbi3.name).getOrThrow().kbInfo() shouldBe kbi3
    }

    @Test
    fun `kb for name is case insensitive`() {
        val kbi1 = app.createKB("Glucose", false)
        val kbi2 = app.createKB("Lipids", false)
        val kbi3 = app.createKB("TFT", false)
        app.kbForName("glucose").getOrThrow().kbInfo() shouldBe kbi1
        app.kbForName("LIPIDS").getOrThrow().kbInfo() shouldBe kbi2
        app.kbForName("tft").getOrThrow().kbInfo() shouldBe kbi3
    }

    @Test
    fun `kb for name is case sensible`() {
        val kbi1 = app.createKB("Glucose", true)
        val kbi2 = app.createKB("GLUCOSE", true)
        val kbi3 = app.createKB("glucose", true)
        app.kbForName("Glucose").getOrThrow().kbInfo() shouldBe kbi1
        app.kbForName("GLUCOSE").getOrThrow().kbInfo() shouldBe kbi2
        app.kbForName("glucose").getOrThrow().kbInfo() shouldBe kbi3
        app.kbForName("GLUcose").exceptionOrNull()!!.message shouldBe "More than one KB with name GLUcose found."
    }

    @Test
    fun `unknown kb name`() {
        app.kbForName("Unknown").exceptionOrNull()!!.message shouldBe "No KB with name matching 'Unknown' found."
    }

    @Test
    fun `multiple KBs with same name`() {
        val kbId = app.createKB("KB1", false)
        app.createKB(kbId.name, true)
        app.kbForName(kbId.name).exceptionOrNull()!!.message shouldBe "More than one KB with name ${kbId.name} found."
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
        } catch (_: Exception) {
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
        val caseName = "Whatever"
        app.kbFor(kbi0).kb.addCornerstoneCase(createCase(caseName))
        app.kbFor(kbi0).kb.containsCornerstoneCaseWithName(caseName) shouldBe true

        //When - Existing name, force true.
        val kbi1 = app.createKB(kbName, true)

        //Then
        with(app.kbList()) {
            size shouldBe 2
            this.shouldContain(kbi0)
            get(0).name shouldBe kbName
            get(1).name shouldBe kbName
        }

        app.kbFor(kbi0).kb.containsCornerstoneCaseWithName(caseName) shouldBe true //original kb
        app.kbFor(kbi1).kb.containsCornerstoneCaseWithName(caseName) shouldBe false //new kb with same name
        persistenceProvider.idStore().data().keys shouldBe setOf(kbi0.id, kbi1.id)
    }

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
        val info = app.createKBFromSample(kbName, SampleKB.TSH)
        info.name shouldBe kbName
        app.kbList() shouldBe listOf(info)

        // Check the kb.
        app.kbFor(info).kb.allProcessedCases() shouldHaveSize 34
        app.kbFor(info).kb.ruleTree.size() shouldBe 34

        // Now create another.
        val kbName2 = "Stuff"
        val info2 = app.createKBFromSample(kbName2, SampleKB.TSH_CASES)
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
            app.createKBFromSample(kbName, SampleKB.TSH)
        } catch (_: Exception) {
            //expected
        }

        //Then
        app.kbFor(kbInfo).kb.kbInfo.name shouldBe kbName
        persistenceProvider.idStore().data().keys shouldBe setOf(id0)
    }

    private fun createCase(caseName: String) = CaseTestUtils.createCase(caseName)
}
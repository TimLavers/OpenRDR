package io.rippledown.kb.sample

import io.mockk.mockk
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.server.KBEndpoint
import io.rippledown.util.EntityRetrieval
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class SampleBuilderTest {
    private val kbName = "Whatever"
    private val persistenceProvider = InMemoryPersistenceProvider()
    private val kbManager = KBManager(persistenceProvider, mockk())
    lateinit var endpoint: KBEndpoint

    @BeforeTest
    fun setup() {
        val rootDir = File("kbe")
        val kbInfo = kbManager.createKB(kbName)
        val kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        endpoint = KBEndpoint(kb)
//        FileUtils.cleanDirectory(endpoint.casesDir)
//        FileUtils.cleanDirectory(endpoint.interpretationsDir)
    }

    @AfterTest
    fun cleanup() {
        kbManager.deleteKB(endpoint.kbInfo())
    }
}
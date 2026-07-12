package io.rippledown.standalone

import io.rippledown.kb.export.importKbFromZipFile
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import java.nio.file.Path

class KbLoader(val zippedKB: Path) {
    fun getInterpreter() : StandAloneInterpreter {
        val persistenceProvider = InMemoryPersistenceProvider()
        val kb = importKbFromZipFile(zippedKB,persistenceProvider)
        return StandAloneInterpreter(kb)
    }
}
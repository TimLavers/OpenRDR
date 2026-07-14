package io.rippledown.standalone

import io.rippledown.kb.export.importKbFromZipFile
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.simpleapi.SimpleInterpreterLoader
import java.nio.file.Path

class KbLoader(val zippedKB: Path): SimpleInterpreterLoader {
    override fun getInterpreter() : StandAloneInterpreter {
        val persistenceProvider = InMemoryPersistenceProvider()
        val kb = importKbFromZipFile(zippedKB,persistenceProvider)
        return StandAloneInterpreter(kb)
    }
}
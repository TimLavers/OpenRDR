package io.rippledown.integration.files

import io.rippledown.files.FileSelection
import io.rippledown.files.KbFileDialogs
import io.rippledown.model.KBInfo
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class ScriptedKbFileDialogs : KbFileDialogs {
    private val imports = ConcurrentLinkedQueue<FileSelection>()
    private val exports = ConcurrentLinkedQueue<FileSelection>()
    private val requests = AtomicInteger()
    val requestCount get() = requests.get()

    fun queueImport(selection: FileSelection) {
        imports.add(selection)
    }

    fun queueExport(selection: FileSelection) {
        exports.add(selection)
    }

    override suspend fun chooseImportArchive(): FileSelection {
        requests.incrementAndGet()
        return checkNotNull(imports.poll()) { "No import selection has been configured for this scenario." }
    }

    override suspend fun chooseExportDestination(kbInfo: KBInfo): FileSelection {
        requests.incrementAndGet()
        return checkNotNull(exports.poll()) { "No export selection has been configured for this scenario." }
    }
}

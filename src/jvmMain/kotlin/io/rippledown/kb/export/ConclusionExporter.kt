package io.rippledown.kb.export

import io.rippledown.kb.ConclusionManager
import io.rippledown.model.Conclusion

class ConclusionExporter: Exporter<Conclusion>, Importer<Conclusion> {
    override fun exportToString(t: Conclusion) = "${t.id} ${t.text}"
    override fun importFromString(data: String): Conclusion {
        val parts = data.split(' ')
        return Conclusion(parts[0].toInt(), parts[1])
    }
}
class ConclusionSource(val conclusionManager: ConclusionManager): IdentifiedObjectSource<Conclusion> {
    override fun all() = conclusionManager.all()

    override fun idFor(t: Conclusion) = t.id

    override fun exporter() = ConclusionExporter()

    override fun exportType() = "Conclusion"
}
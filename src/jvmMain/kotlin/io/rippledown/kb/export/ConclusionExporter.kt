package io.rippledown.kb.export

import io.rippledown.kb.ConclusionManager
import io.rippledown.model.Conclusion

class ConclusionExporter: Exporter<Conclusion> {
    override fun serializeAsString(t: Conclusion) = t.text
}
class ConclusionSource(val conclusionManager: ConclusionManager): IdentifiedObjectSource<Conclusion> {
    override fun all() = conclusionManager.all()

    override fun idFor(t: Conclusion) = t.id

    override fun exporter() = ConclusionExporter()

    override fun exportType() = "Conclusion"
}
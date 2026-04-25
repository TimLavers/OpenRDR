package io.rippledown.appbar

import io.rippledown.model.KBInfo
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.sample.SampleKB
import java.io.File

/** Operations on the workspace's set of knowledge bases. */
interface KBControlHandler {
    var selectKB: (id: String) -> Unit
    var createKB: (name: String) -> Unit
    var createKBFromSample: (name: String, sample: SampleKB) -> Unit
    var importKB: (data: File) -> Unit
    var exportKB: (data: File) -> Unit
    val kbList: () -> List<KBInfo>
}

/** Operations on the currently-selected knowledge base. */
interface KbEditControlHandler {
    var setKbDescription: (name: String) -> Unit
    var kbDescription: () -> String
    var lastRuleDescription: () -> UndoRuleDescription
    var undoLastRule: () -> Unit
}

package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.model.rule.RuleTree
import io.rippledown.util.EntityRetrieval
import java.util.*

class KBManager {
    private val kbInfos = mutableSetOf<KBInfo>()

    fun all(): Set<KBInfo> {
        return kbInfos.toSet()
    }

    fun createKB(name: String): KBInfo {
        val id = UUID.randomUUID().toString()
        val result = KBInfo(id, name)
        kbInfos.add(result)
        return result
    }

    fun openKB(id: String): EntityRetrieval<KB> {
        val kbInfo = kbInfos.firstOrNull{
            it.id == id
        }
        return if (kbInfo == null) {
            EntityRetrieval.Failure("Unknown id: $id.")
        } else {
            EntityRetrieval.Success(KB(kbInfo, AttributeManager(), RuleTree()))
        }
    }
}
package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.util.EntityRetrieval

class KBManager(private val persistenceProvider: PersistenceProvider) {
    private val kbInfos = mutableSetOf<KBInfo>()

    init {
        persistenceProvider.idStore().data().forEach{
            val id = it.key
            val kbInfo = persistenceProvider.kbPersistence(id).kbInfo()
            kbInfos.add(kbInfo)
        }
    }

    fun all(): Set<KBInfo> {
        return kbInfos.toSet()
    }

    fun createKB(name: String): KBInfo {
        val result = KBInfo(name)
        persistenceProvider.createKBPersistence(result)
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
            val persistentKB = persistenceProvider.kbPersistence(kbInfo.id)
            EntityRetrieval.Success(KB(persistentKB))
        }
    }
}
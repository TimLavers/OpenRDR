package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.util.EntityRetrieval
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KBManager(private val persistenceProvider: PersistenceProvider) {
    private val kbInfos = mutableSetOf<KBInfo>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java.name)

    init {
        persistenceProvider.idStore().data().forEach{
            val id = it.key
            try {
                val kbInfo = persistenceProvider.kbPersistence(id).kbInfo()
                kbInfos.add(kbInfo)
            } catch (e: Exception) {
                // todo test for this
                logger.warn("Could not open KB for $it, as shown.", e)
            }
        }
    }

    fun all(): Set<KBInfo> {
        return kbInfos.toSet()
    }

    fun createKB(name: String, force: Boolean = false): KBInfo {
        if (!force) {
            val existingKBInfo = kbInfos.firstOrNull { it.name.equals(name, true) }
            if (existingKBInfo != null) {
                throw IllegalArgumentException("A KB with name ${existingKBInfo.name} already exists. Use force=true to create a KB with the same name, ignoring case, as an existing KB.")
            }
        }
        val result = KBInfo(name)
        persistenceProvider.createKBPersistence(result)
        kbInfos.add(result)
        logger.info("KBManager; KB with name: '${result.name}' and id: '${result.id}'")
        return result
    }

    fun deleteKB(kbInfo: KBInfo) {
        val idOfKBToBeDeleted = kbInfos.firstOrNull{ it.id == kbInfo.id} ?: throw IllegalArgumentException("No KB with id $kbInfo was found.")
        persistenceProvider.destroyKBPersistence(idOfKBToBeDeleted)
        kbInfos.remove(idOfKBToBeDeleted)
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
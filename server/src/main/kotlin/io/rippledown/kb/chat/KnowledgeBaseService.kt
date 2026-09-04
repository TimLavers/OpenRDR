package io.rippledown.kb.chat

import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase

enum class DemonstrationCase { Pathology, Minimal }

/**
 * What a knowledge base management chat action is allowed to do. These
 * operations are about the set of knowledge bases, not the contents of one, so
 * they sit above [RuleService]. See documentation/design/kb_management_by_chat.md.
 */
interface KnowledgeBaseService {
    fun knowledgeBases(): List<KBInfo>
    fun openKnowledgeBase(): KBInfo?
    fun resolve(name: String): KbResolution
    fun nearDuplicateOf(newName: String): KBInfo?
    suspend fun open(kbInfo: KBInfo)
    suspend fun create(name: String): KBInfo
    suspend fun close()
    suspend fun delete(kbInfo: KBInfo)
    suspend fun addDemonstrationCase(kind: DemonstrationCase): RDRCase
    fun isRuleSessionActive(): Boolean
}

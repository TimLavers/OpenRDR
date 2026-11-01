package io.rippledown.kb.chat

import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.PresentUser
import io.rippledown.model.change.ChangeRecord
import io.rippledown.sample.SampleKB

/**
 * What a knowledge base management chat action is allowed to do. These
 * operations manage knowledge bases or query their history and presence without
 * requiring a current case. They sit above [RuleService]. See documentation/design/kb_management.md.
 */
interface KnowledgeBaseService {
    fun knowledgeBases(): List<KBInfo>
    fun demonstrations(): List<SampleKB>
    fun isDemonstrationTitle(name: String): Boolean
    fun openKnowledgeBase(): KBInfo?
    fun resolve(name: String): KbResolution
    fun nearDuplicateOf(newName: String): KBInfo?
    suspend fun open(kbInfo: KBInfo)
    suspend fun create(name: String): KBInfo
    suspend fun createFromSample(name: String, sample: SampleKB): KBInfo
    suspend fun close()
    suspend fun delete(kbInfo: KBInfo): KBInfo?
    suspend fun addDemonstrationCase(): RDRCase
    suspend fun rename(newName: String): KBInfo
    fun description(kbInfo: KBInfo): String
    fun setDescription(kbInfo: KBInfo, text: String)
    fun isRuleSessionActive(): Boolean
    /** Newest first; an unknown attribute name is refused rather than returning unrelated history. */
    fun recentChanges(limit: Int, attributeName: String?): List<ChangeRecord>
    /** Connected users on the open KB, sorted by user ID, excluding this user. */
    fun usersPresent(): List<PresentUser>
}

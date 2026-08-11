package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentKB
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class ConclusionMigrationTest {
    private lateinit var persistentKB: PersistentKB
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        persistentKB = InMemoryKB(KBInfo("id123", "Blah"))
        kb = KB(persistentKB)
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(name: String, glucoseValue: String? = "12.0"): RDRCase =
        with(RDRCaseBuilder()) {
            glucoseValue?.let { addValue(glucose(), defaultDate, it) }
            build(name)
        }

    private fun addConclusionRule(
        text: String,
        variables: List<CommentVariable> = emptyList(),
        parent: Rule = kb.ruleTree.root
    ): Rule {
        val conclusion = kb.conclusionManager.getOrCreate(text, variables)
        return kb.ruleManager.createRuleAndAddToParent(
            parent, conclusion, setOf(greaterThanOrEqualTo(null, glucose(), 11.0))
        )
    }

    /** The comments given for the case by the pre-migration machinery. */
    private fun oldComments(case: RDRCase): Set<String> =
        kb.interpret(case).conclusions()
            .map { it.render(case) { id -> kb.attributeManager.getById(id) }.text }
            .toSet()

    /** The comment attribute values for the case after migration. */
    private fun newComments(case: RDRCase): Set<String> {
        val viewable = kb.viewableCase(kb.addProcessedCase(case))
        return kb.attributeManager.commentAttributes()
            .mapNotNull { viewable.case.latestValue(it) }
            .toSet()
    }

    private fun migrateAndReload() {
        migrateConclusionsToCommentAttributes(persistentKB)
        kb = KB(persistentKB)
    }

    @Test
    fun `a conclusion rule becomes a by-definition rule on an auto-named comment attribute`() {
        // Given a KB with a conclusion rule
        val rule = addConclusionRule("Glucose is high.")
        val persistentBefore = persistentKB.ruleStore().all().first { it.id == rule.id }

        // When it is migrated
        migrateAndReload()

        // Then a comment attribute holds the conclusion text as its definition
        val comment = kb.attributeManager.byName("C1")!!
        comment.kind shouldBe AttributeKind.COMMENT
        kb.derivedDefinitionManager.definitionFor(comment.id) shouldBe CommentTemplate("Glucose is high.")

        // And the rule is rewritten in place: same id, parent and conditions
        val persistentAfter = persistentKB.ruleStore().all().first { it.id == rule.id }
        persistentAfter shouldBe persistentBefore.copy(
            conclusionId = null,
            assignment = AssignValue(comment, ByDefinition)
        )

        // And the conclusion store is empty
        persistentKB.conclusionStore().all() shouldBe emptySet()
    }

    @Test
    fun `the comments given for a case are unchanged by migration`() {
        // Given a KB with a conclusion rule and the comments it gives for cases
        addConclusionRule("Glucose is high.")
        val before = oldComments(createCase("A"))
        before shouldBe setOf("Glucose is high.")

        // When it is migrated
        migrateAndReload()

        // Then the migrated rules give the same comments
        newComments(createCase("A")) shouldBe before
        newComments(createCase("B", "5.0")) shouldBe emptySet()
    }

    @Test
    fun `comment variables are substituted identically after migration`() {
        // Given a conclusion with a variable, and its rendering for a case
        addConclusionRule("Glucose is \${} today.", listOf(CommentVariable(glucose().id)))
        val before = oldComments(createCase("A"))
        before shouldBe setOf("Glucose is 12.0 today.")

        // When it is migrated
        migrateAndReload()

        // Then the substitution is unchanged
        newComments(createCase("A")) shouldBe before
    }

    @Test
    fun `unresolved comment variables render identically after migration`() {
        // Given a conclusion with a variable that has no value in the case
        val weight = kb.attributeManager.getOrCreate("Weight")
        addConclusionRule("Weight is \${}.", listOf(CommentVariable(weight.id)))
        val before = oldComments(createCase("A"))
        before shouldBe setOf("Weight is {Weight: no value}.")

        // When it is migrated
        migrateAndReload()

        // Then the no-value marker is unchanged
        newComments(createCase("A")) shouldBe before
    }

    @Test
    fun `a stopping rule is untouched and still retracts the comment`() {
        // Given a conclusion rule with a stopping child rule
        val parent = addConclusionRule("Glucose is high.")
        kb.ruleManager.createRuleAndAddToParent(
            parent, null, setOf(greaterThanOrEqualTo(null, glucose(), 20.0))
        )

        // When it is migrated
        migrateAndReload()

        // Then the stopper still retracts for such cases only
        newComments(createCase("A", "25.0")) shouldBe emptySet()
        newComments(createCase("B")) shouldBe setOf("Glucose is high.")
    }

    @Test
    fun `a replacement rule gets its own comment attribute and still replaces`() {
        // Given a conclusion rule with a replacing child rule
        val parent = addConclusionRule("Glucose is high.")
        val replacement = kb.conclusionManager.getOrCreate("Glucose is very high.")
        kb.ruleManager.createRuleAndAddToParent(
            parent, replacement, setOf(greaterThanOrEqualTo(null, glucose(), 20.0))
        )

        // When it is migrated
        migrateAndReload()

        // Then the replacement applies for such cases only
        newComments(createCase("A", "25.0")) shouldBe setOf("Glucose is very high.")
        newComments(createCase("B")) shouldBe setOf("Glucose is high.")
    }

    @Test
    fun `migration is idempotent`() {
        // Given a migrated KB
        addConclusionRule("Glucose is high.")
        migrateAndReload()
        val attributesAfterFirst = kb.attributeManager.all()
        val rulesAfterFirst = persistentKB.ruleStore().all()

        // When it is migrated again
        migrateAndReload()

        // Then nothing changes
        kb.attributeManager.all() shouldBe attributesAfterFirst
        persistentKB.ruleStore().all() shouldBe rulesAfterFirst
    }

    @Test
    fun `migration of a KB with no conclusions is a no-op`() {
        // Given a KB with no conclusion rules
        val attributesBefore = kb.attributeManager.all()
        val rulesBefore = persistentKB.ruleStore().all()

        // When it is migrated
        migrateAndReload()

        // Then nothing changes
        kb.attributeManager.all() shouldBe attributesBefore
        persistentKB.ruleStore().all() shouldBe rulesBefore
    }
}

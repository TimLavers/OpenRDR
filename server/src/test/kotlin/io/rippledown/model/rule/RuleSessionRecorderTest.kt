package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.persistence.inmemory.InMemoryRuleSessionRecordStore
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleSessionRecorderTest : RuleTestBase() {
    private lateinit var recorder: RuleSessionRecorder
    private lateinit var conclusionFactory: DummyConclusionFactory


    @BeforeTest
    fun setup() {
        recorder = RuleSessionRecorder(InMemoryRuleSessionRecordStore())
        conclusionFactory = DummyConclusionFactory()
    }

    @Test
    fun `ids of most recently added rules with no rules yet added`() {
        recorder.idsOfRulesAddedInMostRecentSession() shouldBe null
    }

    @Test
    fun `record rule`() {
        val conclusionA = conclusionFactory.getOrCreate("A")
        val ruleGivingA = Rule(5, null, conclusionA)
        recorder.recordRuleSessionCommitted(setOf(ruleGivingA))

        recorder.idsOfRulesAddedInMostRecentSession()!!.idsOfRulesAddedInSession shouldBe setOf(ruleGivingA.id)
        val conclusionB = conclusionFactory.getOrCreate("A")
        val rule1GivingB = Rule(6, null, conclusionB)
        val rule2GivingB = Rule(7, null, conclusionB)
        val rule3GivingB = Rule(8, null, conclusionB)
        recorder.recordRuleSessionCommitted(setOf(rule1GivingB, rule2GivingB, rule3GivingB))
        recorder.idsOfRulesAddedInMostRecentSession()!!.idsOfRulesAddedInSession shouldBe setOf(rule1GivingB.id, rule2GivingB.id, rule3GivingB.id)
    }

    @Test
    fun `all known rule ids`() {
//        recorder.idsOfAllSessionRules() shouldBe setOf()
//
//        val a = conclusionFactory.getOrCreate("A")
//        val addA = Rule(5, null, a)
//        recorder.recordRuleSessionCommitted(setOf(addA))
//        recorder.idsOfAllSessionRules() shouldBe setOf(addA.id)
//
//        val b = conclusionFactory.getOrCreate("B")
//        val addB = Rule(6, null, b)
//        recorder.recordRuleSessionCommitted(setOf(addB))
//        recorder.idsOfAllSessionRules() shouldBe setOf(addA.id, addB.id)
//
//        val removeA = Rule(7, addA,null)
//        recorder.recordRuleSessionCommitted(setOf(removeA))
//        recorder.idsOfAllSessionRules() shouldBe setOf(addA.id, addB.id, removeA.id)
//
//        val addBAgain = Rule(8, null, b)
//        recorder.recordRuleSessionCommitted(setOf(addBAgain))
//        recorder.idsOfAllSessionRules() shouldBe setOf(addA.id, addB.id, removeA.id, addBAgain.id)
//
//        val removeB1 = Rule(9, addB,null)
//        val removeB2 = Rule(10, addBAgain,null)
//        recorder.recordRuleSessionCommitted(setOf(removeB1, removeB2))
//        recorder.idsOfAllSessionRules() shouldBe setOf(addA.id, addB.id, removeA.id, addBAgain.id, removeB1.id, removeB2.id)
    }
}
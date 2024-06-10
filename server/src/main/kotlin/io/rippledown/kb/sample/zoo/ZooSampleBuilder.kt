package io.rippledown.kb.sample.zoo

import io.rippledown.kb.sample.SampleRuleBuilder
import io.rippledown.server.KBEndpoint

class ZooSampleBuilder(private val kbe: KBEndpoint) {
    fun buildRules() {
        setupCases()
        SingleClassificationZooKBRuleBuilder(kbe).buildRules()
    }

    fun setupCases() {
        createAttributes()
        ZooCases(kbe.kb.attributeManager).cases().forEach {
            kbe.kb.addProcessedCase(it)
        }
    }

    private fun createAttributes() {
        // We create the attributes ahead of time and set their order
        // so that the order in the case view is well-defined.
        val attributeNamesInOrder = listOf("age", "prescription", "astigmatism", "tearProduction")
        val attributesInOrder = attributeNamesInOrder.map {
            kbe.getOrCreateAttribute(it)
        }
        kbe.setAttributeOrder(attributesInOrder)
    }
}
class SingleClassificationZooKBRuleBuilder(kbe: KBEndpoint) : SampleRuleBuilder(kbe) {
    private val milk = kbe.getOrCreateAttribute("milk")
    private val aquatic = kbe.getOrCreateAttribute("aquatic")
    private val feathers = kbe.getOrCreateAttribute("feathers")
    private val backbone = kbe.getOrCreateAttribute("backbone")
    private val breathes = kbe.getOrCreateAttribute("breathes")
    private val legs = kbe.getOrCreateAttribute("legs")
    private val fins = kbe.getOrCreateAttribute("fins")
    private val eggs = kbe.getOrCreateAttribute("eggs")
    private val tail = kbe.getOrCreateAttribute("tail")
    private val t = "true"
    private val f = "false"
    private val mammal = "mammal"
    private val fish = "fish"
    private val bird = "bird"
    private val mollusc = "mollusc"
    private val insect = "insect"
    private val amphibian = "amphibian"
    private val reptile = "reptile"
    private val milkTrue = kbe.getOrCreateCondition(isCondition(milk, t))
    private val milkFalse = kbe.getOrCreateCondition(isCondition(milk, f))
    private val isAquatic = kbe.getOrCreateCondition(isCondition(aquatic, t))
    private val hasFeathers = kbe.getOrCreateCondition(isCondition(feathers, t))
    private val featherless = kbe.getOrCreateCondition(isCondition(feathers, f))
    private val spineless = kbe.getOrCreateCondition(isCondition(backbone, f))
    private val hasSpine = kbe.getOrCreateCondition(isCondition(backbone, t))
    private val doesNotBreathe = kbe.getOrCreateCondition(isCondition(breathes, f))
    private val legless = kbe.getOrCreateCondition(isCondition(legs, "0"))
    private val atLeast4Legs = kbe.getOrCreateCondition(greaterThanOrEqualTo(legs, 4.0))
    private val sixLegs = kbe.getOrCreateCondition(isCondition(legs, "6"))
    private val finless = kbe.getOrCreateCondition(isCondition(fins, f))
    private val laysEggs = kbe.getOrCreateCondition(isCondition(eggs, t))
    private val doesBreathe = kbe.getOrCreateCondition(isCondition(breathes, t))
    private val hasTail = kbe.getOrCreateCondition(isCondition(tail, t))

    fun buildRules() {
        addCommentForCase("aardvark", mammal, milkTrue)
        addCommentForCase("bass", fish, isAquatic)
        addCommentForCase("chicken", bird, hasFeathers)
        addCommentForCase("clam", mollusc, spineless, doesNotBreathe, legless)
        replaceCommentForCase("crab", fish, mollusc, spineless, finless)
        removeCommentForCase("dolphin", fish, milkTrue)
        removeCommentForCase("duck", fish, hasFeathers)
        addCommentForCase("flea", insect, laysEggs, spineless, doesBreathe)
        replaceCommentForCase("frog", fish, amphibian, doesBreathe, milkFalse, featherless)
        addCommentForCase("pitviper", reptile, hasSpine, doesBreathe, legless)
        removeCommentForCase("dolphin", reptile, milkTrue)
        addCommentForCase("scorpion", mollusc, spineless, doesBreathe, atLeast4Legs)
        replaceCommentForCase("seasnake", fish, reptile, hasSpine, featherless, finless, legless)
        replaceCommentForCase("slug", insect, mollusc, legless)
        replaceCommentForCase("termite", mollusc, insect, sixLegs)
        addCommentForCase("tortoise", reptile, laysEggs, hasSpine, hasTail, atLeast4Legs)
        removeCommentForCase("newt", reptile, isAquatic, atLeast4Legs)
    }
}
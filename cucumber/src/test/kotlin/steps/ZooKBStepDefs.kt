package steps

import io.cucumber.java8.En
import io.rippledown.examples.zoo.ZooCases
import io.rippledown.integration.RestClientAttributeFactory
import io.rippledown.integration.RestClientRuleBuilder
import io.rippledown.integration.restclient.RESTClient

@Suppress("unused") // Used in cucumber file.
class ZooKBStepDefs : En {
    init {
        When("the Zoo KB cases have been loaded") {
            val attributeFactory = RestClientAttributeFactory(labProxy().restProxy)
            val caseMaker = ZooCases(attributeFactory)
            labProxy().restProxy.setAttributeOrder(caseMaker.attributesInOrder())
            caseMaker.cases().forEach {
                labProxy().provideCase(it)
            }
        }

        And("the single classification Zoo KB rules have been built") {
            SingleClassificationZooKBRuleBuilder(restClient()).buildRules()
        }
    }
}
class SingleClassificationZooKBRuleBuilder(restClient: RESTClient): RestClientRuleBuilder(restClient) {
    private val milk = attributeFactory.create("milk")
    private val aquatic = attributeFactory.create("aquatic")
    private val feathers = attributeFactory.create("feathers")
    private val backbone = attributeFactory.create("backbone")
    private val breathes = attributeFactory.create("breathes")
    private val legs = attributeFactory.create("legs")
    private val fins = attributeFactory.create("fins")
    private val eggs = attributeFactory.create("eggs")
    private val t = "true"
    private val f = "false"
    private val mammal = "mammal"
    private val fish = "fish"
    private val bird = "bird"
    private val mollusc = "mollusc"
    private val insect = "insect"
    private val amphibian = "amphibian"
    private val reptile = "reptile"
    private val milkTrue = conditionFactory.getOrCreate(isCondition(milk, t))
    private val milkFalse = conditionFactory.getOrCreate(isCondition(milk, f))
    private val isAquatic = conditionFactory.getOrCreate(isCondition(aquatic, t))
    private val hasFeathers = conditionFactory.getOrCreate(isCondition(feathers, t))
    private val featherless = conditionFactory.getOrCreate(isCondition(feathers, f))
    private val spineless = conditionFactory.getOrCreate(isCondition(backbone, f))
    private val hasSpine = conditionFactory.getOrCreate(isCondition(backbone, t))
    private val doesNotBreathe = conditionFactory.getOrCreate(isCondition(breathes, f))
    private val legless = conditionFactory.getOrCreate(isCondition(legs, "0"))
    private val finless = conditionFactory.getOrCreate(isCondition(fins, f))
    private val laysEggs = conditionFactory.getOrCreate(isCondition(eggs, t))
    private val doesBreathe = conditionFactory.getOrCreate(isCondition(breathes, t))

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
    }
}
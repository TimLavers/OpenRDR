package steps

import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.*
import java.time.Duration

/**
 * Prints "Scenario N of M" as each scenario starts. Cucumber parses every
 * selected feature before it runs the first scenario, so the total is known
 * in time. Registered with `--plugin steps.ScenarioProgress`.
 *
 * The total counts every scenario in the parsed features, before `--tags`
 * is applied, so a tag-filtered run sets [HIDE_TOTAL_PROPERTY] to omit it.
 */
class ScenarioProgress : ConcurrentEventListener {
    companion object {
        const val HIDE_TOTAL_PROPERTY = "scenarioProgress.hideTotal"
    }

    private val hideTotal = System.getProperty(HIDE_TOTAL_PROPERTY).toBoolean()
    private var total = 0
    private var started = 0
    private val startedAt = mutableMapOf<String, java.time.Instant>()

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestSourceParsed::class.java) { event ->
            total += event.nodes.sumOf { countScenarios(it) }
        }
        publisher.registerHandlerFor(TestCaseStarted::class.java) { event ->
            started++
            startedAt[event.testCase.id.toString()] = event.instant
            val ofTotal = if (hideTotal) "" else " of $total"
            println("\nScenario $started$ofTotal: '${event.testCase.name}'")
        }
        publisher.registerHandlerFor(TestCaseFinished::class.java) { event ->
            val began = startedAt.remove(event.testCase.id.toString())
            val seconds = began?.let { Duration.between(it, event.instant).seconds } ?: 0
            println("Scenario ${event.result.status}. Duration: $seconds seconds")
        }
    }

    private fun countScenarios(node: Node): Int = when (node) {
        is Node.Scenario -> 1
        is Node.Example -> 1
        is Node.Container<*> -> node.elements().sumOf { countScenarios(it) }
        else -> 0
    }
}

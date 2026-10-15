package steps

import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.*
import java.io.File
import java.time.Duration

/**
 * Prints "Scenario N of M" as each scenario starts. Cucumber parses every
 * selected feature before it runs the first scenario, so the total is known
 * in time. Registered with `--plugin steps.ScenarioProgress`.
 *
 * The total counts every scenario in the parsed features, before `--tags`
 * is applied, so a tag-filtered run sets [HIDE_TOTAL_PROPERTY] to omit it.
 *
 * Each finished scenario is also written to `build/timings/<folder>.csv` with
 * its duration and the number of Gemini calls the server made, and the run
 * ends with the totals, so that the cost of the suite can be tracked.
 */
class ScenarioProgress : ConcurrentEventListener {
    companion object {
        const val HIDE_TOTAL_PROPERTY = "scenarioProgress.hideTotal"

        /** Set by the dry run, whose zero timings would otherwise overwrite real ones. */
        const val NO_TIMINGS_PROPERTY = "scenarioProgress.noTimings"
    }

    private val hideTotal = System.getProperty(HIDE_TOTAL_PROPERTY).toBoolean()
    private val writeTimings = !System.getProperty(NO_TIMINGS_PROPERTY).toBoolean()
    private var total = 0
    private var started = 0
    private val startedAt = mutableMapOf<String, java.time.Instant>()
    private val timingsFile = ScenarioTimingsFile(File("build/timings"))
    private var totalSeconds = 0L
    private var totalGeminiCalls = 0
    private var finished = 0

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
            val geminiCalls = StepsInfrastructure.geminiCallsThisScenario()
            println("Scenario ${event.result.status}. Duration: $seconds seconds. Gemini calls: $geminiCalls")
            finished++
            totalSeconds += seconds
            totalGeminiCalls += geminiCalls
            if (writeTimings) timingsFile.append(
                ScenarioTiming(
                    folder = featureFolderOf(event.testCase.uri),
                    feature = featureNameOf(event.testCase.uri),
                    scenario = event.testCase.name,
                    status = event.result.status.toString(),
                    seconds = seconds,
                    geminiCalls = geminiCalls
                )
            )
        }
        publisher.registerHandlerFor(TestRunFinished::class.java) {
            println("\n$finished scenarios in ${totalSeconds / 60}m ${totalSeconds % 60}s, $totalGeminiCalls Gemini calls")
        }
    }

    private fun countScenarios(node: Node): Int = when (node) {
        is Node.Scenario -> 1
        is Node.Example -> 1
        is Node.Container<*> -> node.elements().sumOf { countScenarios(it) }
        else -> 0
    }
}

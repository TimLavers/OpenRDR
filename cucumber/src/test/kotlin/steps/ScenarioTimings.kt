package steps

import java.io.File
import java.net.URI

/**
 * One scenario's measurements, written as a CSV line so that the folder runs
 * (each its own JVM) can be summed over a whole `cucumberTest`.
 */
data class ScenarioTiming(
    val folder: String,
    val feature: String,
    val scenario: String,
    val status: String,
    val seconds: Long,
    val geminiCalls: Int
) {
    fun csvLine() = listOf(folder, feature, scenario, status, seconds.toString(), geminiCalls.toString())
        .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }

    companion object {
        const val CSV_HEADER = "folder,feature,scenario,status,seconds,geminiCalls"
    }
}

/** The requirements folder a feature lives in, e.g. `chat` for `.../requirements/chat/X.feature`. */
fun featureFolderOf(featureUri: URI): String {
    val segments = featureUri.path.trimEnd('/').split('/')
    val requirements = segments.lastIndexOf("requirements")
    return if (requirements in 0 until segments.lastIndex - 1) segments[requirements + 1] else "unknown"
}

fun featureNameOf(featureUri: URI): String = featureUri.path.substringAfterLast('/').removeSuffix(".feature")

/**
 * Append-only, because a folder's scenarios can span two JVMs: the gradle folder task reruns the
 * failures in a second one. The task deletes the file before the first, next to its rerun file.
 */
class ScenarioTimingsFile(private val directory: File) {
    fun append(timing: ScenarioTiming) {
        directory.mkdirs()
        val file = File(directory, "${timing.folder}.csv")
        if (!file.exists()) file.writeText(ScenarioTiming.CSV_HEADER + "\n")
        file.appendText(timing.csvLine() + "\n")
    }
}

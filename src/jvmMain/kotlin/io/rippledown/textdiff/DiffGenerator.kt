package io.rippledown.textdiff

import com.github.difflib.text.DiffRowGenerator
import io.rippledown.model.diff.*

const val START_ADD = "+"
const val START_REMOVE = "-"
const val END_ADD = ""
const val END_REMOVE = ""


fun diffs(original: String, revised: String): List<Diff> {

    val generator: DiffRowGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(false)
        .oldTag { f -> if (f) START_REMOVE else END_REMOVE }
        .newTag { f -> if (f) START_ADD else END_ADD }
        .mergeOriginalRevised(true)
        .build()

    val originalLines = original.map {
        it.toString()
    }
    val revisedLines = revised.map {
        it.toString()
    }


    val diffRows = generator.generateDiffRows(originalLines, revisedLines)
    diffRows.forEach {
        println("old row        = ${it.oldLine}")
    }

    val diffs = mutableListOf<Diff>()
    diffRows.map { diffRow ->
        val text = diffRow.oldLine
        if (text.startsWith(START_ADD)) {
            //will be in the form +X
            diffs.add(Addition(text.substring(1)))
        } else if (text.startsWith(START_REMOVE)) {
            if (text.length == 4) {
                //will be in the form -X+Y
                diffs.add(Replacement(text.substring(1, 2), text.substring(3)))
            } else {
                //will be in the form -X
                diffs.add(Removal(text.substring(1)))
            }
        } else {
            diffs.add(Unchanged(text))
        }
    }
    return diffs
}


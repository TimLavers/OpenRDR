package io.rippledown.integration.proxy

/**
 * The server logs one "Gemini API call #N" line per request (`callWithTimeout` in the llm
 * module). Lines are counted rather than the highest N read, because a server restarted
 * mid-scenario appends to the same log with its counter back at 1.
 */
private val GEMINI_CALL_LINE = Regex("""Gemini API call #\d+""")

fun geminiCallsLogged(logLines: Iterable<String>): Int = logLines.count { GEMINI_CALL_LINE.containsMatchIn(it) }

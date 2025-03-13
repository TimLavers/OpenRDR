package io.rippledown.model.rule

fun parseToIds(text: String): Set<Int> = text.split(' ').map { it.trim().toInt() }.toSet()

data class RuleSessionRecord(val id: Int?, val index: Int, val idsOfRulesAddedInSession: Set<Int>) {
    fun idsString() = idsOfRulesAddedInSession.joinToString(" ")
}
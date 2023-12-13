package io.rippledown.util

/**
 * Generic result to avoid using exceptions for control flow,
 * as descrived here: https://elizarov.medium.com/kotlin-and-exceptions-8062f589d07
 */
sealed class EntityRetrieval<T>(val ok: Boolean) {
    data class Success<T>(val entity: T): EntityRetrieval<T>(true)
    data class Failure<T>(val errorMessage: String): EntityRetrieval<T>(false)
}
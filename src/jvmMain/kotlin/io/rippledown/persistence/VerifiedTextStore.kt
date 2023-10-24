package io.rippledown.persistence

interface VerifiedTextStore {
    /**
     * @return the verified text for a case with the specified id, or null if the verified text has not been set
     */
    fun get(id: Long): String?

    fun put(id: Long, text: String)
}
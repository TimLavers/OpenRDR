package io.rippledown.model

interface AttributeFactory {
    fun create(name: String): Attribute
}
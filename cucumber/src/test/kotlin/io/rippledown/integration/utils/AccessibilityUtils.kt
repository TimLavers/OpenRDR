package io.rippledown.integration.utils

import javax.accessibility.AccessibleContext

fun AccessibleContext.dumpToText(componentDepth: Int = 0) {
    val childCount = accessibleChildrenCount
    val name = this.accessibleName
    val role = this.accessibleRole.toDisplayString()
    val description = this.accessibleDescription
    println("Name: $name, role: $role, description: $description, componentDepth: $componentDepth, child count: $childCount ")
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.dumpToText(componentDepth + 1)
    }
}

fun AccessibleContext.findByDescriptionInTree(descriptionText: String): AccessibleContext? {
    val description = this.accessibleDescription
    if (descriptionText == description) return this
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        val result = getAccessibleChild(i).accessibleContext.findByDescriptionInTree(descriptionText)
        if (result != null) return result
    }
    return null
}

class AccessibilityUtils {
}
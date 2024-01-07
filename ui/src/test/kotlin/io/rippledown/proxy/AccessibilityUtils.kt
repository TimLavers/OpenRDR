package io.rippledown.proxy

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

class AccessibilityUtils {
}
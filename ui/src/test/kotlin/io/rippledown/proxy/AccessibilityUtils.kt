package io.rippledown.proxy

import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

fun AccessibleContext.find(description: String, role: AccessibleRole): AccessibleContext? {
//    println("find, this.name: ${this.accessibleName}, this.descr: ${this.accessibleDescription}, this.role: ${this.accessibleRole}")
    val nameMatch = description == this.accessibleDescription
//    println("nameMatch: $nameMatch")
    if (nameMatch && role == this.accessibleRole) return this
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        val child = getAccessibleChild(i).accessibleContext.find(description, role)
        if (child != null) return child
    }
    return null
}
fun AccessibleContext.dumpToText(componentDepth: Int = 0, showNulls: Boolean = false) {
    val childCount = accessibleChildrenCount
    val name = this.accessibleName
    val role = this.accessibleRole.toDisplayString()
    val description = this.accessibleDescription
    if (showNulls || name != null || description != null) {
        println("Name: $name, role: $role, description: $description, componentDepth: $componentDepth, child count: $childCount ")
    }
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.dumpToText(componentDepth + 1, showNulls)
    }
}

fun ComposeWindow.isReadyForTesting(): Boolean {
    return this.isActive && this.isEnabled && this.isFocusable
}
fun ComposeWindow.waitForWindowToShow() {
    var loop = 0
    while (!isReadyForTesting() && loop++ < 50) {
        Thread.sleep(100)
    }
}

fun findComposeDialogThatIsShowing(): ComposeDialog? {
    val allWindows = java.awt.Window.getWindows()
    val d = allWindows.firstOrNull{w -> w is ComposeDialog }
    return if (d == null) null else d as ComposeDialog
}

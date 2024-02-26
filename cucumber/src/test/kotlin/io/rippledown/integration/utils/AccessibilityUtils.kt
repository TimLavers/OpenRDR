package io.rippledown.integration.utils

import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

fun AccessibleContext.find(description: String, role: AccessibleRole): AccessibleContext? {
    val matcher = { context: AccessibleContext ->
        description == context.accessibleDescription && role == context.accessibleRole
    }
    return this.find(matcher)
}
fun AccessibleContext.find(description: String): AccessibleContext? {
    val matcher = { context: AccessibleContext ->
        description == context.accessibleDescription
    }
    return this.find(matcher)
}

fun AccessibleContext.waitTillFound(description: String, role: AccessibleRole): AccessibleContext? {
    val matcher = { context: AccessibleContext ->
        description == context.accessibleDescription && role == context.accessibleRole
    }
    return this.find(matcher)
}
fun AccessibleContext.findByName(name: String, role: AccessibleRole): AccessibleContext? {
    println("Find by name: '$name', role: '$role'")
    val matcher = { context: AccessibleContext ->
        val nameMatch = name == context.accessibleName
        println("nameMatch: $nameMatch, context accessible name: '${context.accessibleName}'.")
        val roleMatch = role == context.accessibleRole
        println("roleMatch: $roleMatch")
        (name == context.accessibleName && role == context.accessibleRole)
    }
    return this.find(matcher, true)
}
fun AccessibleContext.find(matcher: (AccessibleContext) -> Boolean, debug: Boolean = false): AccessibleContext? {
    if (debug) println("find, this.name: ${this.accessibleName}, this.description: ${this.accessibleDescription}, this.role: ${this.accessibleRole}")
    if (matcher(this)) return this
    val childCount = accessibleChildrenCount
    if (debug) println("Searching amongst children, of which there are $childCount")
    for (i in 0..<childCount) {
        val child = getAccessibleChild(i).accessibleContext.find(matcher, debug)
        if (child != null) return child
    }
    return null
}
fun AccessibleContext.findAllByDescriptionPrefix(prefix: String): Set<AccessibleContext> {
    val matcher = { context: AccessibleContext ->
        if (context.accessibleDescription == null) false else context.accessibleDescription.startsWith(prefix)
    }
    return this.findAll(matcher)
}
fun AccessibleContext.findAll(matcher: (AccessibleContext) -> Boolean, debug: Boolean = false): Set<AccessibleContext> {
    val result = mutableSetOf<AccessibleContext>()
    this.findAll(result, matcher, debug)
    return result
}
fun AccessibleContext.findAll(holder: MutableSet<AccessibleContext>, matcher: (AccessibleContext) -> Boolean, debug: Boolean = false) {
    if (matcher(this)) holder.add(this)
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.findAll(holder, matcher, debug)
    }
}

fun AccessibleContext.findLabelChildren(): List<String> {
    val result = mutableListOf<String>()
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        val child = getAccessibleChild(i)
        if (child.accessibleContext.accessibleRole == AccessibleRole.LABEL) {
            val caseName = child.accessibleContext.accessibleName
            //TODO. Why is this necessary? It seems that the same name is added multiple times.
            if (!result.contains(caseName)) result.add(caseName)
        }
    }
    return result
}
fun AccessibleContext.dumpToText(index: Int, componentDepth: Int = 0, ignoreNulls: Boolean = true) {
    val childCount = accessibleChildrenCount
    val name = accessibleName
    val role = accessibleRole.toDisplayString()
    val description = accessibleDescription
    if (!ignoreNulls || name != null || description != null) {
        println("Index: $index, Name: $name, role: $role, description: $description, componentDepth: $componentDepth, child count: $childCount ")
    }
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.dumpToText(i, componentDepth + 1, ignoreNulls)
    }
}
fun AccessibleContext.findAndClick(description: String) {
    val expandDropdownButton = find(description, AccessibleRole.PUSH_BUTTON)
    val action = expandDropdownButton!!.accessibleAction
    action.doAccessibleAction(0)
}
fun AccessibleContext.printActions() {
    val actions = this.accessibleAction
    val count = actions.accessibleActionCount
    println("Number of actions: $count")
    for (i in 0..<count) {
        println("Action $i: ${actions.getAccessibleActionDescription(i)}")
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
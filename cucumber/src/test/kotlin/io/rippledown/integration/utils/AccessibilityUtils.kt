package io.rippledown.integration.utils

import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import io.kotest.matchers.shouldNotBe
import io.rippledown.integration.waitUntilAsserted
import net.sourceforge.tess4j.Tesseract
import org.assertj.swing.edt.GuiActionRunner.execute
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleText


/**
 * Reads the rendered text from a Compose Text node.
 *
 * From Compose 1.11 the Java accessibility bridge uses contentDescription as
 * the accessible name on Text nodes, overriding the rendered text. So
 * `accessibleName` on (e.g.) an AttributeCell returns
 * "Header for case data row Einstein 0" rather than "MCV".
 * `AccessibleText` exposes the actual displayed characters and is the
 * supported way to recover the rendered text.
 *
 * Falls back to `accessibleName` for nodes without `AccessibleText`.
 */
fun renderedText(ctx: AccessibleContext): String {
    val text = ctx.accessibleText ?: return ctx.accessibleName ?: ""
    return buildString {
        for (i in 0 until text.charCount) {
            val ch = text.getAtIndex(AccessibleText.CHARACTER, i)
            if (ch != null) append(ch)
        }
    }
}

fun captureComponentScreenshot(context: AccessibleContext): BufferedImage? {
    val accessibleComponent = context.accessibleComponent ?: return null
    val screenLocation = accessibleComponent.locationOnScreen ?: return null
    val size = accessibleComponent.size ?: return null
    val screenRect = Rectangle(screenLocation.x, screenLocation.y, size.width, size.height)
    return Robot().createScreenCapture(screenRect)
}
object TesseractInstallation {
    val tesseract: Tesseract = Tesseract()
    init {
        tesseract.setDatapath("/opt/homebrew/opt/tesseract/share/tessdata")
    }

    fun getText(image: BufferedImage): String? {
        return tesseract.doOCR(image)
    }
}
fun getComponentTextUsingOCR(context: AccessibleContext?): List<String> {
    if (context == null) return emptyList()
    val image = captureComponentScreenshot(context) ?: return emptyList()

    val text = TesseractInstallation.getText(image)
    println("Extracted text from screenshot: $text")

    return text?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
}

fun AccessibleContext.find(description: String, role: AccessibleRole): AccessibleContext? {
    val matcher = { context: AccessibleContext ->
        description == context.accessibleDescription && role == context.accessibleRole
    }
    return find(matcher)
}


fun waitForContextToBeNotNull(contextProvider: () -> AccessibleContext, description: String) {
    waitUntilAsserted { execute<AccessibleContext?> { contextProvider().find(description) } shouldNotBe null }
}

fun AccessibleContext.find(description: String, debug: Boolean = false): AccessibleContext? {
    val matcher = { context: AccessibleContext? ->
        context != null && context.accessibleDescription != null && context.accessibleDescription.contains(description)
    }
    return find(matcher, debug)
}

fun AccessibleContext.findExact(description: String, debug: Boolean = false): AccessibleContext? {
    val matcher = { context: AccessibleContext? ->
        context != null && context.accessibleDescription == description
    }
    return find(matcher, debug)
}

fun AccessibleContext.findByName(name: String, role: AccessibleRole): AccessibleContext? {
    val matcher = { context: AccessibleContext ->
        (name == context.accessibleName && role == context.accessibleRole)
    }
    return find(matcher)
}

fun AccessibleContext.find(matcher: (AccessibleContext) -> Boolean, debug: Boolean = false): AccessibleContext? {
    if (debug) println("find, name: '${accessibleName}', description: '${accessibleDescription}', role: '${accessibleRole}'")
    if (matcher(this)) return this
    val childCount = accessibleChildrenCount
    if (debug) println("Searching amongst children, of which there are $childCount")
    for (i in 0..<childCount) {
        try {
            val child = getAccessibleChild(i).accessibleContext.find(matcher, debug)
            if (child != null) return child
        } catch (_: Exception) {
            //ignore. This is a workaround for a possible bug in the Java AccessibleContext API
        }
    }
    return null
}

fun AccessibleContext.findAllByDescriptionPrefix(prefix: String): Set<AccessibleContext> {
    val matcher = { context: AccessibleContext ->
        if (context.accessibleDescription == null) false else {
            context.accessibleDescription.startsWith(prefix)
        }
    }
    return findAll(matcher)
}

fun AccessibleContext.findAll(matcher: (AccessibleContext) -> Boolean, debug: Boolean = false): Set<AccessibleContext> {
    val result = mutableSetOf<AccessibleContext>()
    this.findAll(result, matcher, debug)
    return result
}

fun AccessibleContext.findAll(
    holder: MutableSet<AccessibleContext>,
    matcher: (AccessibleContext) -> Boolean,
    debug: Boolean = false
) {
    if (matcher(this)) {
        holder.add(this)
    }
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        try {
            getAccessibleChild(i).accessibleContext.findAll(holder, matcher, debug)
        } catch (_: Exception) {
            //ignore. This is a workaround for a possible bug in the Java AccessibleContext API
        }
    }
}

/**
 * Search the descendant tree for a LABEL node whose rendered text equals
 * [text]. Use this instead of [findByName] when the underlying Compose 1.11
 * accessibility bridge prefixes the LABEL's `accessibleName` with the parent's
 * contentDescription (which would defeat an exact-name match).
 */
fun AccessibleContext.findLabelByRenderedText(text: String): AccessibleContext? {
    if (accessibleRole == AccessibleRole.LABEL && renderedText(this) == text) {
        return this
    }
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        try {
            val match = getAccessibleChild(i).accessibleContext.findLabelByRenderedText(text)
            if (match != null) return match
        } catch (_: Exception) {
            // Same defensive ignore as findAll: the AccessibleContext API
            // can throw when traversing concurrently rebuilt subtrees.
        }
    }
    return null
}

fun AccessibleContext.findLabelChildren(): List<String> {
    val result = mutableListOf<String>()
    val childCount = accessibleChildrenCount
    for (i in 0..<childCount) {
        val child = getAccessibleChild(i)
        if (child.accessibleContext.accessibleRole == AccessibleRole.LABEL) {
            // Compose 1.11's accessibility bridge propagates a parent's
            // contentDescription down to merged child Text nodes, so reading
            // accessibleName returns "<contentDescription><name>" rather
            // than just the rendered text. Read the visible characters via
            // AccessibleText (renderedText) instead.
            val caseName = renderedText(child.accessibleContext)
            //TODO. Why is this necessary? It seems that the same name is added multiple times.
            if (!result.contains(caseName)) result.add(caseName)
        }
    }
    return result
}

fun AccessibleContext.dumpToText(index: Int = 0, componentDepth: Int = 0, ignoreNulls: Boolean = true) {
    val childCount = accessibleChildrenCount
    val name = accessibleName
    val role = accessibleRole.toDisplayString()
    val description = accessibleDescription
    if (!ignoreNulls || name != null || description != null) {
        val indent = "  ".repeat(componentDepth)
        println("$indent Index: $index, Name: $name, role: $role, description: $description, componentDepth: $componentDepth, child count: $childCount ")
    }
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.dumpToText(i, componentDepth + 1, ignoreNulls)
    }
}

fun AccessibleContext.findAndClick(description: String) {
    val expandDropdownButton = find(description)
    expandDropdownButton!!.accessibleAction.doAccessibleAction(0)
}

fun AccessibleContext.findAndClickRadioButton(description: String) {
    val button = find(description, AccessibleRole.RADIO_BUTTON)
    val action = button!!.accessibleAction
    action.doAccessibleAction(0)
}

fun AccessibleContext.printActions() {
    val actions = this.accessibleAction
    if (actions == null) {
        println("actions is null")
        return
    }
    val count = actions.accessibleActionCount
    println("Number of actions: $count")
    for (i in 0..<count) {
        println("Action $i: ${actions.getAccessibleActionDescription(i)}")
    }
}

fun ComposeWindow.isReadyForTesting(): Boolean {
    return this.isActive && this.isEnabled && this.isFocusable
}

/**
 * Performs an OS-level mouse click at the centre of the component backing this
 * AccessibleContext. Unlike [javax.accessibility.AccessibleAction.doAccessibleAction],
 * which invokes the click callback without moving native focus, a Robot mouse
 * click moves OS-level focus onto the target compose element so that subsequent
 * Robot key presses are delivered to it.
 */
fun AccessibleContext.mouseClickAtCentre() {
    val component = accessibleComponent ?: return
    val location = execute<java.awt.Point?> {
        try {
            component.locationOnScreen
        } catch (_: Exception) {
            null
        }
    } ?: return
    val size = execute<java.awt.Dimension?> { component.size } ?: return
    val cx = location.x + size.width / 2
    val cy = location.y + size.height / 2
    val robot = java.awt.Robot()
    robot.mouseMove(cx, cy)
    Thread.sleep(80)
    robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
    Thread.sleep(80) // Compose needs a non-zero press duration to treat this as a tap
    robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
    Thread.sleep(80)
}

fun ComposeWindow.waitForWindowToShow() {
    var loop = 0
    while (!isReadyForTesting() && loop++ < 50) {
        Thread.sleep(100)
    }
}

fun waitForComposeDialogToShow(): ComposeDialog {
    var loop = 0
    var dialog: ComposeDialog? = null
    while (dialog == null && loop++ < 50) {
        Thread.sleep(100)
        dialog = findComposeDialogThatIsShowing()
    }
    require(dialog != null) {
        "No dialog found"
    }
    return dialog
}

fun findComposeDialogThatIsShowing(): ComposeDialog? {
    val allWindows = java.awt.Window.getWindows()
    val d = allWindows.firstOrNull { w -> w is ComposeDialog && w.isShowing }
    return if (d == null) null else d as ComposeDialog
}

fun AccessibleContext.dumpToText(componentDepth: Int = 0, showNulls: Boolean = false) {
    val childCount = accessibleChildrenCount
    val name = this.accessibleName
    val role = this.accessibleRole.toDisplayString()
    val description = this.accessibleDescription
    if (showNulls || name != null || description != null) {
        println("\nName: $name, role: $role, description: $description, componentDepth: $componentDepth, child count: $childCount ")
    }
    this.accessibleAction?.let {
        val count = it.accessibleActionCount
        println("Number of actions: $count")
        for (i in 0..<count) {
            println("Action $i: ${it.getAccessibleActionDescription(i)}")
        }
    }
    for (i in 0..<childCount) {
        getAccessibleChild(i).accessibleContext.dumpToText(componentDepth + 1, showNulls)
    }
}
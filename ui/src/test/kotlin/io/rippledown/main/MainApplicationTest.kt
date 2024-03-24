package io.rippledown.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.kb.KB_CONTROL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.TITLE
import io.rippledown.proxy.dumpToText
import io.rippledown.proxy.find
import io.rippledown.proxy.findComposeDialogThatIsShowing
import io.rippledown.proxy.waitForWindowToShow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Assume.assumeFalse
import org.junit.Before
import java.awt.GraphicsEnvironment
import javax.accessibility.AccessibleRole
import javax.swing.SwingUtilities
import kotlin.test.Test
import androidx.compose.ui.window.launchApplication as realLaunchApplication

class MainApplicationTest {

    lateinit var handler: Handler

    @Before
    fun setUp() {
        handler = mockk<Handler>(relaxed = true)
        every { handler.isClosing } returns { true }
    }

    @Test
    fun blah() {
        var windowHolder: ComposeWindow? = null

        val runnable = java.lang.Runnable {
            run {
                application {
                    Window(
                        onCloseRequest = ::exitApplication,
                        icon = painterResource("water-wave-icon.png"),
                        title = TITLE
                    ) {
                        windowHolder = this.window
                        OpenRDRUI(handler)
                    }
                }
            }
        }
        Thread(runnable, "App Runner").start()

        while (windowHolder == null) {
            Thread.sleep(1000)
        }
        val window = windowHolder!!

        window.waitForWindowToShow()
        println("------------ TEST READY!! -------------")


        val accessibleContext0 = window.accessibleContext
//        accessibleContext0.dumpToText(showNulls = true)

//        Thread.sleep(50_000)

        val kbRow = accessibleContext0.find(KB_CONTROL_DESCRIPTION, AccessibleRole.UNKNOWN)

        println("kbRow: $kbRow")

        val expandDropdownButton = kbRow!!.find(KB_CONTROL_DROPDOWN_DESCRIPTION, AccessibleRole.PUSH_BUTTON)
        println("eDB: $expandDropdownButton")

        val action = expandDropdownButton!!.accessibleAction
        val count = action.accessibleActionCount
        println("action count: $count")
        count shouldBe 1
        val actionDescription = action.getAccessibleActionDescription(0)
        println("action description: $actionDescription")

        action.doAccessibleAction(0)
        println("clicked, init")

        Thread.sleep(1_000)

        val accessibleContext1 = window.accessibleContext
//        accessibleContext1.dumpToText()

        val dropDown = accessibleContext1.find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
        println("dropDown: $dropDown")

        val createKBItem = dropDown!!.find(CREATE_KB_TEXT, AccessibleRole.PUSH_BUTTON)
        println("createKBItem: $createKBItem")
        val createKBActionCount = createKBItem!!.accessibleAction.accessibleActionCount
        createKBActionCount shouldBe 1
        createKBItem.accessibleAction.doAccessibleAction(0)

        Thread.sleep(1_000)
        println("-----------------------------------------------------")
//        window.accessibleContext.dumpToText()
        Thread.sleep(1_000)

        val dialog = findComposeDialogThatIsShowing()
        println("=========================================================== dialog: $dialog")
        dialog!!.accessibleContext.dumpToText()

    }

    @Test
    fun `run RD`() = runApplicationTest {
        lateinit var window: ComposeWindow

        SwingUtilities.invokeLater {
            launchTestApplication {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(300.dp, 300.dp),
                    )
                ) {
                    window = this.window

                    OpenRDRUI(handler)
                }
            }
        }
        awaitIdle()
        val name = window.accessibleContext.accessibleName
        println("------- name: $name")
        awaitIdle()


    }

    @Test
    fun `run application`() = runApplicationTest {
        var isInit = false
        var isDisposed = false

        val appJob = launchTestApplication {
            DisposableEffect(Unit) {
                isInit = true
                onDispose {
                    isDisposed = true
                }
            }
        }


        appJob.join()

//        assertThat(isInit).isTrue()
//        assertThat(isDisposed).isTrue()
    }

    @org.junit.Test
    fun `run two applications`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen1 by mutableStateOf(true)
        var isOpen2 by mutableStateOf(true)

        launchTestApplication {
            if (isOpen1) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        launchTestApplication {
            if (isOpen2) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(300.dp, 300.dp),
                    )
                ) {
                    window2 = this.window
                    Box(Modifier.size(32.dp).background(Color.Blue))
                }
            }
        }

        awaitIdle()
//        assertThat(window1?.isShowing).isTrue()
//        assertThat(window2?.isShowing).isTrue()

        isOpen1 = false
        awaitIdle()
//        assertThat(window1?.isShowing).isFalse()
//        assertThat(window2?.isShowing).isTrue()

        isOpen2 = false
        awaitIdle()
//        assertThat(window1?.isShowing).isFalse()
//        assertThat(window2?.isShowing).isFalse()
    }

}


suspend fun awaitEDT() {
    // Most of the work usually is done after the first yield(), almost all the work -
    // after fourth yield()
    repeat(100) {
        yield()
    }
}


internal fun runApplicationTest(
    /**
     * Use delay additionally to `yield` in `await*` functions
     *
     * Set this property only if you sure that you can't easily make the test deterministic
     * (non-flaky).
     *
     * We have to use `useDelay` in some Linux Tests, because Linux can behave in
     * non-deterministic way when we change position/size very fast (see the snippet below).
     */
    useDelay: Boolean = false,
    delayMillis: Long = 500,
    // TODO ui-test solved this issue by passing InfiniteAnimationPolicy to CoroutineContext. Do the same way here
    /**
     * Hint for `awaitIdle` that the content contains animations (ProgressBar, TextField cursor, etc).
     * In this case, we use `delay` instead of waiting for state changes to end.
     */
    hasAnimations: Boolean = false,
    animationsDelayMillis: Long = 500,
    timeoutMillis: Long = 30000,
    body: suspend WindowTestScope.() -> Unit
) {
    assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

    runBlocking(MainUIDispatcher) {
        withTimeout(timeoutMillis) {
            val exceptionHandler = TestExceptionHandler()
            withExceptionHandler(exceptionHandler) {
                val scope = WindowTestScope(
                    scope = this,
                    delayMillis = if (useDelay) delayMillis else -1,
                    animationsDelayMillis = if (hasAnimations) animationsDelayMillis else -1,
                    exceptionHandler = exceptionHandler)
                try {
                    scope.body()
                } finally {
                    scope.exitTestApplication()
                }
            }
            exceptionHandler.throwIfCaught()
        }
    }
}

private inline fun withExceptionHandler(
    handler: Thread.UncaughtExceptionHandler,
    body: () -> Unit
) {
    val old = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().uncaughtExceptionHandler = handler
    try {
        body()
    } finally {
        Thread.currentThread().uncaughtExceptionHandler = old
    }
}

internal class TestExceptionHandler : Thread.UncaughtExceptionHandler {
    private var exception: Throwable? = null

    fun throwIfCaught() {
        exception?.also {
            throw it
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (exception != null) {
            exception?.addSuppressed(throwable)
        } else {
            exception = throwable
        }
    }
}

internal class WindowTestScope(
    private val scope: CoroutineScope,
    private val delayMillis: Long,
    private val animationsDelayMillis: Long,
    private val exceptionHandler: TestExceptionHandler
) : CoroutineScope by CoroutineScope(scope.coroutineContext + Job()) {
    var isOpen by mutableStateOf(true)
    private val initialRecomposers = Recomposer.runningRecomposers.value

    fun launchTestApplication(
        content: @Composable ApplicationScope.() -> Unit
    ) = realLaunchApplication {
        if (isOpen) {
            content()
        }
    }

    // Overload `launchApplication` to prohibit calling it from tests
    @Deprecated(
        "Do not use `launchApplication` from tests; use `launchTestApplication` instead",
        level = DeprecationLevel.ERROR
    )
    fun launchApplication(
        @Suppress("UNUSED_PARAMETER") content: @Composable ApplicationScope.() -> Unit
    ): Nothing {
        error("Do not use `launchApplication` from tests; use `launchTestApplication` instead")
    }

    suspend fun exitTestApplication() {
        isOpen = false
        awaitIdle()  // Wait for the windows to actually complete disposing
    }

    suspend fun awaitIdle() {
        if (delayMillis >= 0) {
            delay(delayMillis)
        }

        awaitEDT()

        Snapshot.sendApplyNotifications()

        if (animationsDelayMillis >= 0) {
            delay(animationsDelayMillis)
        } else {
            for (recomposerInfo in Recomposer.runningRecomposers.value - initialRecomposers) {
                recomposerInfo.state.takeWhile { it > Recomposer.State.Idle }.collect()
            }
        }

        exceptionHandler.throwIfCaught()
    }
}
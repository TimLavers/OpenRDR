package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mui.base.BadgeUnstyledProps
import mui.material.Badge
import mui.material.BadgeColor
import mui.material.Button
import mui.material.ButtonProps
import mui.system.sx
import proxy.findById
import proxy.requireBadgeCount
import proxy.requireNoBadge
import react.FC
import react.ReactNode
import react.VFC
import react.create
import react.dom.createRootFor
import web.cssom.px
import kotlin.test.Test

external interface TestBadgeHandler : BadgeUnstyledProps {
    var count: Int
    var childNode: ReactNode
}

val TestBadge = FC<TestBadgeHandler> { handler ->
    Badge {
        badgeContent = handler.count.unsafeCast<ReactNode>()
        color = BadgeColor.primary
        showZero = false
        sx {
            marginTop = 10.px
        }
        child(handler.childNode)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeTest {

    @Test
    fun shouldReadBadgeContent(): TestResult {
        return runTest {
            val vfc = VFC {
                TestBadge {
                    count = 42
                }
            }
            with(createRootFor(vfc)) {
                requireBadgeCount(42)
            }
        }
    }

    @Test
    fun shouldHideBadgeIfContentIsZero(): TestResult {
        return runTest {
            val vfc = VFC {
                TestBadge {
                    count = 0
                }
            }
            with(createRootFor(vfc)) {
                requireNoBadge()
            }
        }
    }

    @Test
    fun shouldRenderChildContent(): TestResult {
        val button = FC<ButtonProps> {
            Button {
                id = "button_id"
                +"Go to Bondi"
            }
        }.create()

        return runTest {
            val vfc = VFC {
                TestBadge {
                    count = 42
                    childNode = button
                }
            }
            with(createRootFor(vfc)) {
                findById("button_id").textContent shouldBe "Go to Bondi"
            }
        }
    }
}

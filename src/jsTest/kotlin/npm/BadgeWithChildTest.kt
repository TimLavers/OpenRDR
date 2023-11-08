package npm

import io.kotest.matchers.shouldBe
import io.rippledown.interpretation.requireBadgeCount
import io.rippledown.interpretation.requireNoBadge
import kotlinx.coroutines.test.TestResult
import mui.material.Badge
import mui.material.Button
import mui.material.ButtonProps
import proxy.findById
import react.FC
import react.ReactNode
import react.create
import react.dom.test.runReactTest
import kotlin.test.Test

class BadgeWithChildTest {

    @Test
    fun shouldReadBadgeContent(): TestResult {
        val vfc = FC {
            Badge {
                badgeContent = 42.unsafeCast<ReactNode>()
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                requireBadgeCount(42)
            }
        }
    }

    @Test
    fun shouldHideBadgeIfContentIsZero(): TestResult {
        val vfc = FC {
            Badge {
                badgeContent = 0.unsafeCast<ReactNode>()
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                requireNoBadge()
            }
        }
    }

    @Test
    fun shouldRenderChildContent(): TestResult {
        var numberOfClicks = 0
        val button = FC<ButtonProps> {
            Button {
                id = "button_id"
                +"Go to Bondi"
                onClick = {
                    ++numberOfClicks
                }
            }
        }.create()

        val vfc = FC {
            Badge {
                badgeContent = 42.unsafeCast<ReactNode>()
                +button
            }
        }
        return runReactTest(vfc) { container ->
            with(container) {
                val buttonElement = findById("button_id")
                buttonElement.textContent shouldBe "Go to Bondi"
                numberOfClicks shouldBe 0
                buttonElement.click()
                numberOfClicks shouldBe 1
            }
        }
    }
}


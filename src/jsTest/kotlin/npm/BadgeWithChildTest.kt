package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mui.material.Badge
import mui.material.Button
import mui.material.ButtonProps
import proxy.findById
import io.rippledown.interpretation.requireBadgeCount
import io.rippledown.interpretation.requireNoBadge
import react.FC
import react.ReactNode
import react.VFC
import react.create
import react.dom.createRootFor
import kotlin.test.Test

class BadgeWithChildTest {

    @Test
    fun shouldReadBadgeContent(): TestResult {
        return runTest {
            val vfc = VFC {
                Badge {
                    badgeContent = 42.unsafeCast<ReactNode>()
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
                Badge {
                    badgeContent = 0.unsafeCast<ReactNode>()
                }
            }
            with(createRootFor(vfc)) {
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

        return runTest {
            val vfc = VFC {
                Badge {
                    badgeContent = 42.unsafeCast<ReactNode>()
                    +button
                }
            }
            with(createRootFor(vfc)) {
                val buttonElement = findById("button_id")
                buttonElement.textContent shouldBe "Go to Bondi"
                numberOfClicks shouldBe 0
                buttonElement.click()
                numberOfClicks shouldBe 1
            }
        }
    }
}

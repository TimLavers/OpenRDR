package npm

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import mui.material.Button
import mui.material.ButtonProps
import proxy.findById
import proxy.requireBadgeCount
import proxy.requireNoBadge
import react.FC
import react.VFC
import react.create
import react.dom.createRootFor
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BadgeWithChildTest {

    @Test
    fun shouldReadBadgeContent(): TestResult {
        return runTest {
            val vfc = VFC {
                BadgeWithChild {
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
                BadgeWithChild {
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
                BadgeWithChild {
                    count = 42
                    childNode = button
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

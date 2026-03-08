package io.rippledown.interpretation

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.utils.createViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationViewHandler

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
    }

    @Test
    fun `should show interpretation text`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }
}
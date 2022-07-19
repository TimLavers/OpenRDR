package io.rippledown.integration

import org.openqa.selenium.By
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BasicElementsTest: UITestBase() {

    @BeforeTest
    fun setup() {
        setupWebDriver()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    @Test
    fun basicElements() {
        val headingElement = driver.findElement(By.id("main_heading"))
        assertEquals(headingElement.text, "Open RippleDown")
    }
}
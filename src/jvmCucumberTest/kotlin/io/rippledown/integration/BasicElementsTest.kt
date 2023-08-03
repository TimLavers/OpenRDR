package io.rippledown.integration

import org.openqa.selenium.By
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BasicElementsTest : io.rippledown.integration.UITestBase() {

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupWebDriver()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    @Test
    fun basicElements() {
        val headingElement = driver.findElement(By.id("main_heading"))
        assertEquals(headingElement.text, "Open RippleDown")
    }
}
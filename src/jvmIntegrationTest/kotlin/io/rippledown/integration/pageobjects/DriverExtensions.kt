package io.rippledown.integration.pageobjects

import org.awaitility.Awaitility
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Duration

fun WebDriver.waitForElementWithId(id: String): WebElement {
        Awaitility.await().atMost(Duration.ofSeconds(3)).until {
            this.findElement(By.id(id)) != null
        }
        return this.findElement(By.id(id))
    }

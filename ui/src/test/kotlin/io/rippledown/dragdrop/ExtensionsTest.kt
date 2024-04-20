package io.rippledown.dragdrop

import io.kotest.matchers.shouldBe
import org.junit.Test

class ExtensionsTest {
    @Test
    fun `move items in list`() {
        with(mutableListOf('a')) {
            this.move(0, 0)
            this shouldBe mutableListOf('a')
        }
        with(mutableListOf('a', 'b')) {
            this.move(0, 0)
            this shouldBe mutableListOf('a', 'b')
            this.move(1, 1)
            this shouldBe mutableListOf('a', 'b')
            this.move(1, 0)
            this shouldBe mutableListOf( 'b', 'a')
        }
        with(mutableListOf('a', 'b', 'c')) {
            this.move(0, 2)
            this shouldBe mutableListOf( 'b', 'c', 'a')
            this.move(0, 1)
            this shouldBe mutableListOf( 'c', 'b', 'a')
        }
    }
}
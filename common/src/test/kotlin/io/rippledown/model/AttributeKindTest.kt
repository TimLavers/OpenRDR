package io.rippledown.model

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

internal class AttributeKindTest {

    @Test
    fun `only EXTERNAL attributes are not assigned by the KB`() {
        // Given each kind
        // Then only the KB-assigned kinds report as such
        AttributeKind.EXTERNAL.isAssignedByKB() shouldBe false
        AttributeKind.DERIVED.isAssignedByKB() shouldBe true
        AttributeKind.COMMENT.isAssignedByKB() shouldBe true
    }

    @Test
    fun `kinds survive serialization`() {
        // Given each kind
        AttributeKind.entries.forEach {
            // When it is serialized and deserialized
            // Then it is unchanged
            serializeDeserialize(it) shouldBe it
        }
    }
}

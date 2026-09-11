package io.rippledown.sample

enum class SampleKB {
    TSH {
        override fun title() = "Thyroid Stimulating Hormone"
    },
    TSH_CASES {
        override fun title() = "Thyroid Stimulating Hormone - cases only"
    },
    CONTACT_LENSES {
        override fun title() = "Contact Lense Prescription"
    },
    CONTACT_LENSES_CASES {
        override fun title() = "Contact Lense Prescription - cases only"
    },
    ZOO {
        override fun title() = "Zoo Animals"
    },
    ZOO_CASES {
        override fun title() = "Zoo Animals - cases only"
    },
    PATHOLOGY {
        override fun title() = "Pathology"
    };

    abstract fun title(): String

    companion object {
        fun demonstrations(): List<SampleKB> = listOf(TSH, CONTACT_LENSES, ZOO, PATHOLOGY)
    }
}

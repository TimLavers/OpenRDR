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
    };

    abstract fun title(): String
}
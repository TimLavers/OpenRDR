package io.rippledown.sample

enum class SampleKB {
    TSH {
        override fun title() = "Thyroid Stimulating Hormone"
    },
    TSH_CASES {
        override fun title() = "Thyroid Stimulating Hormone - cases only"
    };

    abstract fun title(): String
}
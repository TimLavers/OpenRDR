package io.rippledown.sample

enum class SampleKB {
    TSH {
        override fun title() = "Thyroid Stimulating Hormone"
        override fun description() =
            "Interpretative comments for thyroid function test reports, from a published paper. An example only, not for advice or diagnosis."
    },
    TSH_CASES {
        override fun title() = "Thyroid Stimulating Hormone - cases only"
        override fun description() = "The thyroid function test cases only, with no rules."
    },
    CONTACT_LENSES {
        override fun title() = "Contact Lens Prescription"
        override fun description() =
            "Contact lens prescription rules from a UNSW course on machine learning and Ripple-Down Rules. An example only."
    },
    CONTACT_LENSES_CASES {
        override fun title() = "Contact Lens Prescription - cases only"
        override fun description() = "The contact lens cases only, with no rules."
    },
    ZOO {
        override fun title() = "Zoo Animals"
        override fun description() =
            "Classifies animals from their features; the cases and rules from Chapter 5 of Compton and Kang's book on Ripple-Down Rules."
    },
    ZOO_CASES {
        override fun title() = "Zoo Animals - cases only"
        override fun description() = "The zoo animal cases only, with no rules."
    },
    PATHOLOGY {
        override fun title() = "Pathology"
        override fun description() =
            "Three small pathology cases for trying out rule building, cornerstone review, derived attributes and the AI report. No rules are built yet."
    },
    CANCER_GENES {
        override fun title() = "Cancer Genes"
        override fun description() =
            "Simple classifier for genetic mutations that may drive cancer."
    },
    CANCER_GENES_CASES {
        override fun title() = "Cancer Genes - cases only"
        override fun description() = "The Cancer Genes test cases only, with no rules."
    };


    abstract fun title(): String
    abstract fun description(): String

    companion object {
        fun demonstrations(): List<SampleKB> = listOf(TSH, CONTACT_LENSES, ZOO, PATHOLOGY)
    }
}

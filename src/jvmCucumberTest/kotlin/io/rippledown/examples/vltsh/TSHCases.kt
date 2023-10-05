package io.rippledown.examples.vltsh

import io.rippledown.model.AttributeFactory

/**
 * Cases from the paper "Interpretative commenting in clinical chemistry with worked
 * examples for thyroid function test reports" by Vasikaran and Loh.
 *
 * ORD4
 */
class TSHCases(attributeFactory: AttributeFactory) {

    val TSH1 = tshCase {
        name = "1.4.1"
        tsh = "0.67"
        freeT4 = "16"
        age = 28
        clinicalNotes = "Lethargy."
    }.build(attributeFactory)

    val TSH2 = tshCase {
        name = "1.4.2"
        tsh = "0.67"
        age = 28
        clinicalNotes = "Lethargy."
    }.build(attributeFactory)

    val TSH3 = tshCase {
        name = "1.4.3"
        tsh = "0.74"
        freeT4 = "8"
        age = 36
        clinicalNotes = "Weight loss."
    }.build(attributeFactory)

    val TSH4 = tshCase {
        name = "1.4.4"
        tsh = "7.3"
        age = 57
        clinicalNotes = "Weight gain."
    }.build(attributeFactory)

    val TSH5 = tshCase {
        name = "1.4.5"
        tsh = "7.3"
        freeT4 = "13"
        age = 57
        clinicalNotes = "Weight gain."
    }.build(attributeFactory)

    val TSH6 = tshCase {
        name = "1.4.6"
        tsh = "4.5"
        freeT4 = "15"
        sex = "M"
        age = 76
        clinicalNotes = "Routine check."
    }.build(attributeFactory)

    val TSH7 = tshCase {
        name = "1.4.7"
        tsh = "14.0"
        freeT4 = "13"
        age = 62
        clinicalNotes = "Constipation."
    }.build(attributeFactory)

    val TSH8 = tshCase {
        name = "1.4.8"
        tsh = "0.05"
        freeT4 = "13"
        age = 27
        clinicalNotes = "Period of amenorrhea 12/40 weeks."
        location = "Obstetric clinic."
    }.build(attributeFactory)

    val TSH9 = tshCase {
        name = "1.4.9"
        tsh = "4.6"
        freeT4 = "13"
        age = 32
        clinicalNotes = "Trying for a baby."
        testValue {
            attribute = "TPO Antibodies"
            value = "33"
            units = "kU/L"
            upperBound = "6"
        }
    }.build(attributeFactory)

    val TSH10 = tshCase {
        name = "1.4.10"
        tsh = "0.02"
        freeT4 = "18"
        age = 55
        sex = "M"
        clinicalNotes = "Feeling very tired."
    }.build(attributeFactory)
    val TSH11 = tshCase {
        name = "1.4.11"
        tsh = "0.02"
        freeT4 = "18"
        freeT3 = "6.1"
        age = 55
        sex = "M"
        clinicalNotes = "Hyperthyroid?"
    }.build(attributeFactory)
    val TSH12 = tshCase {
        name = "1.4.12"
        tsh = "59"
        freeT4 = "<5"
        age = 74
        sex = "M"
        clinicalNotes = "Hypothyroid?"
    }.build(attributeFactory)
    val TSH13 = multiEpisodeCase(attributeFactory) {
        name = "1.4.13"
        sex = "M"
        dates {
            datesCSL = "2022-08-18T13:07:44.475Z, 2022-08-25T14:22:51.942Z"
        }
        testValues {
            attribute = "TSH"
            units = "mU/L"
            lowerBound = "0.50"
            upperBound = "4.0"
            valuesCSL = "59, 40"
        }
        testValues {
            attribute = "Free T4"
            units = "pmol/L"
            lowerBound = "10"
            upperBound = "20"
            valuesCSL = "<5, 8"
        }
        clinicalNotes {
            values_separated = "Hypothyroid? _ Hypothyroid, started T4 replacement 1 week ago."
        }
        testValues {
            attribute = "Age"
            valuesCSL = "74, 74"
        }
        testValues {
            attribute = "Tests"
            valuesCSL = "TFTs, TFTs"
        }
        testValues {
            attribute = "Patient Location"
            valuesCSL = "General Practice., General Practice."
        }
    }.build()
    val TSH14 = tshCase {
        name = "1.4.14"
        tsh = "0.72"
        freeT4 = "16"
        age = 43
        clinicalNotes = "On T4 replacement."
    }.build(attributeFactory)
    val TSH15 = tshCase {
        name = "1.4.15"
        tsh = "5.6"
        freeT4 = "12"
        age = 54
        clinicalNotes = "On T4 replacement."
    }.build(attributeFactory)
    val TSH16 = tshCase {
        name = "1.4.16"
        tsh = "0.02"
        freeT4 = "19"
        age = 61
        clinicalNotes = "On T4 replacement."
    }.build(attributeFactory)
    val TSH17 = tshCase {
        name = "1.4.17"
        tsh = "0.12"
        freeT4 = "19"
        age = 51
        clinicalNotes = "Previous total thyroidectomy for thyroid cancer. On thyroxine."
    }.build(attributeFactory)
    val TSH18 = multiEpisodeCase(attributeFactory) {
        name = "1.4.18"
        dates {
            datesCSL = "2022-02-25T13:07:44.475Z, 2022-08-18T14:22:51.942Z"
        }
        testValues {
            attribute = "TSH"
            units = "mU/L"
            lowerBound = "0.50"
            upperBound = "4.0"
            valuesCSL = "4.3, 3.6"
        }
        testValues {
            attribute = "Free T4"
            units = "pmol/L"
            lowerBound = "10"
            upperBound = "20"
            valuesCSL = "13, 12"
        }
        clinicalNotes {
            values_separated = " _ Subclinical hypothyroidism, follow-up."
        }
        testValues {
            attribute = "Age"
            valuesCSL = "56, 56"
        }
        testValues {
            attribute = "Tests"
            valuesCSL = "TFTs, TFTs"
        }
    }.build()
    val TSH19 = tshCase {
        name = "1.4.19"
        tsh = "0.03"
        freeT4 = "20"
        age = 37
        clinicalNotes = "Amenorrhea."
    }.build(attributeFactory)
    val TSH20 = tshCase {
        name = "1.4.20"
        tsh = "<0.01"
        freeT4 = "16"
        freeT3 = "5.5"
        age = 53
        clinicalNotes = "Annual check."
    }.build(attributeFactory)
    val TSH21 = multiEpisodeCase(attributeFactory) {
        name = "1.4.21"
        dates {
            datesCSL = "2023-02-14T11:53:44.475Z, 2023-08-04T09:39:51.942Z"
        }
        testValues {
            attribute = "TSH"
            units = "mU/L"
            lowerBound = "0.50"
            upperBound = "4.0"
            valuesCSL = "<0.01, <0.01"
        }
        testValues {
            attribute = "Free T4"
            units = "pmol/L"
            lowerBound = "10"
            upperBound = "20"
            valuesCSL = "16, 17"
        }
        testValues {
            attribute = "Free T3"
            units = "pmol/L"
            lowerBound = "3.0"
            upperBound = "5.5"
            valuesCSL = "5.5, 6.1"
        }
        clinicalNotes {
            values_separated = "Annual check. _ Previous suppressed TSH."
        }
        testValues {
            attribute = "Age"
            valuesCSL = "53, 53"
        }
        testValues {
            attribute = "Tests"
            valuesCSL = "TFTs, TFTs"
        }
    }.build()

}
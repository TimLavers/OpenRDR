package io.rippledown.examples.vltsh

/**
 * Cases from the paper "Interpretative commenting in clinical chemistry with worked
 * examples for thyroid function test reports" by Vasikaran and Loh.
 *
 * ORD4
 */
val TSH1 = tshCase {
    name = "1.4.1"
    tsh = "0.67"
    freeT4 = "16"
    age = 28
    clinicalNotes = "Lethargy."
}.build()

val TSH2 = tshCase {
    name = "1.4.2"
    tsh = "0.67"
    age = 28
    clinicalNotes = "Lethargy."
}.build()

val TSH3 = tshCase {
    name = "1.4.3"
    tsh = "0.74"
    freeT4 = "8"
    age = 36
    clinicalNotes = "Weight loss."
}.build()

val TSH4 = tshCase {
    name = "1.4.4"
    tsh = "7.3"
    age = 57
    clinicalNotes = "Weight gain."
}.build()

val TSH5 = tshCase {
    name = "1.4.5"
    tsh = "7.3"
    freeT4 = "13"
    age = 57
    clinicalNotes = "Weight gain."
}.build()

val TSH6 = tshCase {
    name = "1.4.6"
    tsh = "4.5"
    freeT4 = "15"
    sex = "M"
    age = 76
    clinicalNotes = "Routine check."
}.build()

val TSH7 = tshCase {
    name = "1.4.7"
    tsh = "14.0"
    freeT4 = "13"
    age = 62
    clinicalNotes = "Constipation."
}.build()

val TSH8 = tshCase {
    name = "1.4.8"
    tsh = "0.05"
    freeT4 = "13"
    age = 27
    clinicalNotes = "Period of amenorrhea 12/40 weeks."
    location = "Obstetric clinic."
}.build()

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
        lowerBound = "6"
    }
}.build()

val TSH10 = tshCase {
    name = "1.4.10"
    tsh = "0.02"
    freeT4 = "18"
    age = 55
    sex = "M"
    clinicalNotes = "Feeling very tired."
}.build()
val TSH11 = tshCase {
    name = "1.4.11"
    tsh = "0.02"
    freeT4 = "18"
    freeT3 = "6.1"
    age = 55
    sex = "M"
    clinicalNotes = "Hyperthyroid?"
}.build()
val TSH12 = tshCase {
    name = "1.4.12"
    tsh = "59"
    freeT4 = "<5"
    age = 74
    sex = "M"
    clinicalNotes = "Hypothyroid?"
}.build()
val TSH13 = multiEpisodeCase {
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
}.build()
val TSH15 = tshCase {
    name = "1.4.15"
    tsh = "5.6"
    freeT4 = "12"
    age = 54
    clinicalNotes = "On T4 replacement."
}.build()
val TSH16 = tshCase {
    name = "1.4.16"
    tsh = "0.02"
    freeT4 = "19"
    age = 61
    clinicalNotes = "On T4 replacement."
}.build()
val TSH17 = tshCase {
    name = "1.4.17"
    tsh = "0.12"
    freeT4 = "19"
    age = 51
    clinicalNotes = "Previous total thyroidectomy for thyroid cancer. On thyroxine."
}.build()
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

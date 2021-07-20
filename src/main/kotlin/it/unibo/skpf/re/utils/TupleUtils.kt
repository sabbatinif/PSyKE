package it.unibo.skpf.re.utils

import smile.data.Tuple

fun Tuple.lessOrEqualThan(field: String, value: Double): Boolean =
    this[field].toString().toDouble() <= value

fun Tuple.greaterThan(field: String, value: Double): Boolean =
    this[field].toString().toDouble() > value

fun Tuple.check(field: String): Boolean =
    this[field].toString().toDouble() == 1.0

fun Tuple.checkAll(vararg fields: String): Boolean =
    fields.all { this.check(it) }

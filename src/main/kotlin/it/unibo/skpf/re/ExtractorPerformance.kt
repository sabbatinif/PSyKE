package it.unibo.skpf.re

data class ExtractorPerformance(
    val fidelity: Double,
    val accuracy: Double,
    val ruleNumber: Int,
    val missingPredictions: Double
)

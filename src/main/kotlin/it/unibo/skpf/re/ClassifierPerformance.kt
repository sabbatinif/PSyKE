package it.unibo.skpf.re

/**
 * Performance of a classifier extractor.
 * @property fidelity is the fidelity of the extractor.
 * @property accuracy is the accuracy of the extractor.
 * @property ruleNumber is the number of the extracted rules.
 * @property missingPredictions is the number of predictions
 *                              not provided by the extractor.
 */
data class ClassifierPerformance(
    val fidelity: Double,
    val accuracy: Double,
    val ruleNumber: Int,
    val missingPredictions: Double
)

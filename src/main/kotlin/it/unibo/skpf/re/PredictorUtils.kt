package it.unibo.skpf.re

import smile.classification.Classifier
import smile.classification.DecisionTree
import smile.data.*
import smile.validation.metric.Accuracy
import smile.validation.metric.ConfusionMatrix

fun Array<*>.toInt() =
    this.map { it.toString().toInt() }.toIntArray()

fun accuracy(data: DataFrame, predictor: Classifier<DoubleArray>): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data.inputsArray())).round(2)
}

fun accuracy(data: DataFrame, predictor: Extractor<*, *>): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data).toInt()).round(2)
}

fun accuracy(data: DataFrame, predictor: DecisionTree): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data)).round(2)
}

fun fidelity(
    data: DataFrame,
    predictor: Classifier<DoubleArray>,
    extractor: Extractor<*, *>
): Double {
    return Accuracy.of(predictor.predict(data.inputsArray()), extractor.predict(data).toInt()).round(2)
}

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>,
                    extractor: Extractor<*, *>): ConfusionMatrix {
    return ConfusionMatrix.of(
        predictor.predict(data.inputsArray()),
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it}.toIntArray()
    )
}

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        predictor.predict(data.inputsArray())
    )
}

fun confusionMatrix(data: DataFrame, predictor: DecisionTree): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        predictor.predict(data)
    )
}

fun confusionMatrix(data: DataFrame, extractor: Extractor<*, *>): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it}.toIntArray()
    )
}

fun testClassifier(test: DataFrame, predictor: Classifier<DoubleArray>,
                   printAccuracy: Boolean = true, printMatrix: Boolean = false): Double {
    val accuracy = accuracy(test, predictor)
    if (printAccuracy)
        println("Classifier accuracy: $accuracy")
    if (printMatrix)
        println(confusionMatrix(test, predictor))
    return accuracy
}

fun testClassificationExtractor(name: String, train: DataFrame, test: DataFrame,
                                extractor: Extractor<*, *>,
                                predictor: Classifier<DoubleArray>,
                                printAccuracy: Boolean = true, printMatrix: Boolean = false,
                                printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val fidelity = fidelity(test, predictor, extractor)
    val accuracy = accuracy(test, extractor)
    val missing = 1.0 * extractor.predict(test).count { it == -1 } / test.nrows()
    if (printAccuracy)
        println(name + " " + theory.size +
                " rules with fidelity of " + fidelity +
                " and accuracy of " + accuracy)
    if (printMatrix)
        println(confusionMatrix(test, predictor, extractor))
    if (printRules)
        theory.clauses.forEach { println(it.toString()) }
    return ExtractorPerformance(fidelity, accuracy, theory.size.toInt(), missing)
}

fun testClassificationExtractor(name: String, train: DataFrame, test: DataFrame,
                                extractor: Extractor<Tuple, DecisionTree>,
                                printAccuracy: Boolean = true, printMatrix: Boolean = false,
                                printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val accuracy = accuracy(test, extractor)
    if (printAccuracy)
        println(name + " " + theory.size +
                " rules with accuracy of " + accuracy)
    if (printMatrix)
        println(confusionMatrix(test, extractor))
    if (printRules)
        theory.clauses.forEach { println(it.toString()) }
    return ExtractorPerformance(1.0, accuracy, theory.size.toInt(), 0.0)
}
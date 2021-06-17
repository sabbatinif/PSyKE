package it.unibo.skpf.re.utils

import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.ExtractorPerformance
import it.unibo.skpf.re.cart.CartPredictor
import it.unibo.skpf.re.utils.ClassificationUtils.k
import it.unibo.skpf.re.utils.ClassificationUtils.maxDepth
import it.unibo.skpf.re.utils.ClassificationUtils.maxNodes
import it.unibo.skpf.re.utils.ClassificationUtils.nodeSize
import it.unibo.tuprolog.core.format
import org.apache.commons.csv.CSVFormat
import smile.base.cart.SplitRule
import smile.classification.Classifier
import smile.classification.DecisionTree
import smile.classification.cart
import smile.classification.knn
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.classes
import smile.data.classesArray
import smile.data.formula.Formula
import smile.data.inputs
import smile.data.inputsArray
import smile.data.nCategories
import smile.data.randomSplit
import smile.data.splitFeatures
import smile.data.toBoolean
import smile.io.Read
import smile.validation.metric.Accuracy
import smile.validation.metric.ConfusionMatrix

object ClassificationUtils {
    // cart params
    const val maxDepth = 20
    const val maxNodes = 0
    const val nodeSize = 5

    // knn params
    const val k = 9
}

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

fun confusionMatrix(
    data: DataFrame,
    predictor: Classifier<DoubleArray>,
    extractor: Extractor<*, *>
): ConfusionMatrix {
    return ConfusionMatrix.of(
        predictor.predict(data.inputsArray()),
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it }.toIntArray()
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
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it }.toIntArray()
    )
}

fun testClassifier(
    test: DataFrame,
    predictor: Classifier<DoubleArray>,
    printAccuracy: Boolean = true,
    printMatrix: Boolean = false
): Double {
    val accuracy = accuracy(test, predictor)
    if (printAccuracy)
        println("Classifier accuracy: $accuracy")
    if (printMatrix)
        println(confusionMatrix(test, predictor))
    return accuracy
}

fun testClassifier(
    test: DataFrame,
    predictor: DecisionTree,
    printAccuracy: Boolean = true,
    printMatrix: Boolean = false
): Double {
    val accuracy = accuracy(test, predictor)
    if (printAccuracy)
        println("Classifier accuracy: $accuracy")
    if (printMatrix)
        println(confusionMatrix(test, predictor))
    return accuracy
}

fun testClassificationExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<*, *>,
    predictor: Classifier<DoubleArray>,
    printAccuracy: Boolean = true,
    printMatrix: Boolean = false,
    printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val fidelity = fidelity(test, predictor, extractor)
    val accuracy = accuracy(test, extractor)
    val missing = 1.0 * extractor.predict(test).count { it == -1 } / test.nrows()
    println("\n################################")
    println("# $name extractor")
    println("################################\n")
    if (printAccuracy)
        println(
            theory.size.toString() +
                " rules with fidelity of " + fidelity +
                " and accuracy of " + accuracy
        ).also { println() }
    if (printMatrix)
        println(confusionMatrix(test, predictor, extractor)).also { println() }
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
    return ExtractorPerformance(fidelity, accuracy, theory.size.toInt(), missing)
}

fun testClassificationExtractor(
    name: String,
    train: DataFrame,
    test: DataFrame,
    extractor: Extractor<Tuple, CartPredictor>,
    printAccuracy: Boolean = true,
    printMatrix: Boolean = false,
    printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val accuracy = accuracy(test, extractor)
    println("\n################################")
    println("# $name extractor")
    println("################################\n")
    if (printAccuracy)
        println(theory.size.toString() + " rules with accuracy of " + accuracy).also { println() }
    if (printMatrix)
        println(confusionMatrix(test, extractor)).also { println() }
    if (printRules)
        theory.clauses.forEach { println(it.format(prettyRulesFormatter())) }.also { println() }
    return ExtractorPerformance(1.0, accuracy, theory.size.toInt(), 0.0)
}

@Suppress("UNUSED_VARIABLE")
fun classifyWithoutDiscretise(name: String, testSplit: Double) {
    val dataset = Read.csv("datasets/$name", CSVFormat.DEFAULT.withHeader())
    val (train, test) = dataset.randomSplit(testSplit)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, k)
    val cart = CartPredictor(
        cart(
            Formula.lhs("Class"),
            train.inputs().merge(train.classes()),
            SplitRule.GINI,
            maxDepth, maxNodes, nodeSize
        )
    )
    val cartEx = Extractor.cart(cart)
    testClassificationExtractor("CART without discretisation", train, test, cartEx, true, true, true)
}

fun classify(name: String, testSplit: Double) {
    println("*** $name ***")
    val dataset = Read.csv("datasets/$name", CSVFormat.DEFAULT.withHeader())
    val featureSets = dataset.splitFeatures()
    val (train, test) = dataset.toBoolean(featureSets).randomSplit(testSplit)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, k)
//    saveToFile("irisKNN9.txt", knn)
//    saveToFile("irisTest50.txt", test)
//    saveToFile("irisTrain50.txt", train)
//    saveToFile("irisBoolFeatSet.txt", featureSets)
    testClassifier(test, knn)
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    testClassificationExtractor("REAL", train, test, real, knn, true, true, true)
    val duepan = Extractor.duepan(knn, featureSets)
    testClassificationExtractor("Duepan", train, test, duepan, knn, true, true, true)
    val cart = CartPredictor(
        cart(
            Formula.lhs("Class"),
            train.inputs().merge(train.classes()),
            SplitRule.GINI,
            maxDepth, maxNodes, nodeSize
        )
    )
    val cartEx = Extractor.cart(cart, featureSets)
    testClassificationExtractor("CART", train, test, cartEx, true, true, true)
}

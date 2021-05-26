import smile.classification.Classifier
import smile.data.DataFrame
import smile.validation.metric.Accuracy

fun accuracy(data: DataFrame, predictor: Classifier<DoubleArray>): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data.inputsArray()))
}

fun fidelity(
    data: DataFrame,
    predictor: Classifier<DoubleArray>,
    extractor: Extractor<DoubleArray, Classifier<DoubleArray>>
): Double {
    return Accuracy.of(predictor.predict(data.inputsArray()), extractor.predict(data))
}

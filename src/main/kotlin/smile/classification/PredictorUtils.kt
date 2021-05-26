package smile.classification

import it.unibo.skpf.re.Extractor
import smile.data.DataFrame
import smile.data.classesArray
import smile.data.inputsArray
import smile.validation.metric.Accuracy
import smile.validation.metric.ConfusionMatrix

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

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>,
                    extractor: Extractor<DoubleArray, Classifier<DoubleArray>>): ConfusionMatrix {
    return ConfusionMatrix.of(
        predictor.predict(data.inputsArray()),
        extractor.predict(data)
    )
}

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        predictor.predict(data.inputsArray())
    )
}
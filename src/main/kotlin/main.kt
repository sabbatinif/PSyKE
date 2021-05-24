import smile.classification.*
import smile.io.Read
import smile.validation.metric.Accuracy
import smile.validation.metric.ConfusionMatrix

fun main() {
    val name = "car"

    val dataset = Read.csv("datasets/$name.data")
    val featureSets = dataset.splitFeatures()
    val boolDataset = dataset.toBoolean(featureSets)
    val (train, test) = boolDataset.randomSplit(0.2)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, 9)
    println("Classifier accuracy: " +
            Accuracy.of(test.classesArray(), knn.predict(test.inputsArray())))
    val real = REAL(knn, train, featureSets)
    val realTheory = real.extract(x)
    println("REAL accuracy: " +
            Accuracy.of(knn.predict(test.inputsArray()), real.predict(test)))
    val duepan = Duepan(knn, train, featureSets)
    val duepanTheory = duepan.extract(x)
    println("Duepan accuracy: " +
            Accuracy.of(knn.predict(test.inputsArray()), duepan.predict(test)))

    //realTheory.clauses.forEach { println(it.toString()) }
    println("****")
    //duepanTheory.clauses.forEach { println(it.toString()) }

}

    /*
    val standardiser = standardiser(dataset)
    val scaler = scaler(dataset)

    val sc = scaler.invert(scaler.transform(dataset))
    val st = standardiser.invert(standardiser.transform(dataset))

    for (i in 0 until dataset.ncols())
    {
        println(dataset.column(i))
        println(sc!!.column(i))
        println(st!!.column(i))
    }
    */

    /*
    val model: MLP = mlp(
        x, y,
        arrayOf(Layer.rectifier(25), Layer.mle(3, OutputFunction.SOFTMAX)),
        450,
        TimeFunction.linear(0.05, 10000.0, 0.05))

    val knn = knn(x, y, 3)
    val log = logit(x, y)
    val rbf = rbfnet(x, y, 15)

    val models = listOf(model, knn, log, rbf)

    for (mod in models) {
        println(Accuracy.of(test.classesArray(), mod.predict(test.inputsArray())))
    }

    for (mod in models) {
        for (testInput in test.inputsArray()) {
            print("${mod.predict(testInput)} ")
        }
        println()
    }

    test.classesArray().forEach { print("$it ") }
*/
//    val arti = Data("arti.csv")
//    val model2 = rbfnet(
//        arti.xtrain,
//        arti.ytrain.map { it.toDouble() }.toDoubleArray(),
//        4,
//        true)
//    println(model2.metric(arti.xtest, arti.ytest.map { it.toDouble() }.toDoubleArray()).MAD)
//
//    val model3 = regressor(
//        arti.xtrain,
//        arti.ytrain.map { it.toDouble() }.toDoubleArray(),
//        arrayOf(Layer.tanh(35), Layer.tanh(15), Layer.mse(1, OutputFunction.LINEAR)),
//        200,
//        TimeFunction.linear(0.1, 10000.0, 0.05))
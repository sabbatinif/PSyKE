import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.io.Read
import kotlin.random.Random

class Data (
    filename: String,
    folder: String = "datasets/",
    target: IntArray? = null,
    scale: Boolean = true,
    testSplit: Double = 0.2
) {
    private val seed = 123L
    private var scaler: Scaler? = null
    var labels: Map<Int, String>? = null
    private val dataset: DataFrame = Read.csv("$folder/$filename")
    val nFeat = dataset.ncols() - (target?.size ?: 1)
    val xtrain: Array<DoubleArray>
    val xtest: Array<DoubleArray>
    val ytrain: Array<Number>
    val ytest: Array<Number>

    init {
        val x: MutableList<DoubleArray>
        val y: MutableList<Number>
        if (scale)
            scaler = MinMaxScaler()
        val temp = dataset.drop(nFeat).toArray()
        x = (scaler?.transform(temp) ?: temp).toMutableList()

        if (dataset.types()[nFeat] == DataTypes.StringType) {
            labels = dataset.column(nFeat).toStringArray().distinct().mapIndexed {
                    i, it -> i to it
            }.toMap()
            y = dataset.column(nFeat).toStringArray().map {
                    lab -> labels?.filterValues { it == lab }?.keys?.first() ?: -1
            }.toMutableList()
        }
        else {
            y = dataset.column(nFeat).toDoubleArray().toList().toMutableList()
        }

        x.shuffle(Random(seed))
        y.shuffle(Random(seed))
        val s = (dataset.nrows() * (1 - testSplit)).toInt()
        xtrain = x.subList(0, s).toTypedArray()
        xtest = x.subList(s, x.size).toTypedArray()
        ytrain = y.subList(0, s).toTypedArray()
        ytest = y.subList(s, y.size).toTypedArray()
    }

    private fun toOneHot(classes: Array<Int>): Array<DoubleArray> {
        val oneHot = Array(classes.size) { DoubleArray(classes.distinct().size) {0.0} }
        classes.forEachIndexed { i, it -> oneHot[i][it] = 1.0 }
        return oneHot
    }
}
import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.type.StructType
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector

class BooleanDataFrame(val dataframe: DataFrame) {
    data class Range(
        val mean: Double,
        val std: Double
    ) {
        var lower: Double
        var upper: Double

        init {
            this.lower = this.mean
            this.upper = this.mean
        }
    }

    data class BooleanFeatureSet(
        val name: String,
        val set: Map<String, Pair<Double, Double>>
    )

    val dataset: DataFrame
        get() = this.toBooleanFeatures()

    lateinit var featureSets: MutableSet<BooleanFeatureSet>

    private fun toBooleanFeatures(): DataFrame {
        val outputColumns: MutableList<BaseVector<*, *, *>> = mutableListOf()
        this.featureSets = mutableSetOf()

        for (feature in dataframe.inputs()) {
            outputColumns.addAll(
                when (feature.type()) {
                    DataTypes.StringType -> splitStringFeature(feature.name())
                    DataTypes.DoubleType -> splitDoubleFeature(feature.name())
                    else -> listOf(feature)
                }
            )
        }
        return DataFrame.of(*outputColumns.toTypedArray()).merge(dataframe.outputs())
    }

    private fun splitStringFeature(name: String): ArrayList<BaseVector<*, *, *>> {
        val vectors: ArrayList<BaseVector<*, *, *>> = arrayListOf()
        for (value in dataframe.column(name).toStringArray().distinct()) {
            vectors.add(DoubleVector.of(
                StructField("$name $value", DataTypes.DoubleType),
                dataframe.column(name).toStringArray().map {
                    if (it == value) 1.0 else 0.0
                }.toDoubleArray()
            ))
        }
        return vectors
    }

    private fun splitDoubleFeature(name: String): ArrayList<BaseVector<*, *, *>> {
        val ranges = this.createRanges(
            dataframe.select(
                name, dataframe.column(dataframe.lastColumnIndex).name()
            ), name
        )
        return this.doubleToBoolean(name, ranges)
    }

    private fun doubleToBoolean(name: String, ranges: List<Range>): ArrayList<BaseVector<*, *, *>> {
        fun Double.format(digits: Int) = "%.${digits}f".format(this)
        val vectors: ArrayList<BaseVector<*, *, *>> = arrayListOf()

        BooleanFeatureSet(
            name, mapOf(
                *ranges.map {
                    "$name ${it.lower.format(2)}-${it.upper.format(2)}" to
                            Pair(it.lower, it.upper)
                }.toTypedArray()
            )
        ).apply {
            featureSets.add(this)
            for ((k, v) in this.set) {
                vectors.add(DoubleVector.of(StructField(k, DataTypes.DoubleType),
                    dataframe.column(name).toDoubleArray()
                    .map {
                        if ((v.first <= it) && (it <= v.second)) 1.0 else 0.0
                    }.toDoubleArray()))
            }
        }
        return vectors
    }

    private fun createRanges(data: DataFrame, name: String): List<Range> {
        val ranges = data.categories().map {
            val desc = data.filterByOutput(it).describe()[name]
            Range(desc!!.mean, desc.std)
        }.sortedWith(compareBy{ it.mean })

        ranges.zipWithNext { r1, r2 ->
            while (r1.upper < r2.lower) {
                r1.upper += r1.std
                r2.lower -= r2.std
            }
            val mean = (r1.upper - r1.std + r2.lower + r2.std) / 2
            r1.upper = mean
            r2.lower = mean
        }
        data.describe()[name].apply {
            ranges.first().lower = this!!.min - 0.001
            ranges.last().upper = this.max + 0.001
        }
        return ranges
    }
}
import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.data.type.StructField
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
        lateinit var outputDataframe: DataFrame
        this.featureSets = mutableSetOf()

        dataframe.inputs().schema().fields().apply {
            this.filter { it.isNumeric }
            .map { DataFrame.of(*splitFeature(it.name)) }
            .forEachIndexed { i, it ->
                outputDataframe = if (i == 0) it else outputDataframe.merge(it)
            }
            this.filterNot { it.isNumeric }
                .forEach {
                    outputDataframe = outputDataframe.merge(
                        DataFrame.of(dataframe.inputs().column(it.name))
                    )
                }
        }

        return outputDataframe.merge(dataframe.outputs())
    }

    private fun splitFeature(name: String): Array<BaseVector<*, *, *>> {
        val ranges = this.createRanges(
            dataframe.select(
                name, dataframe.column(dataframe.lastColumnIndex).name()
            ), name
        )
        return this.createFeatureSets(name, ranges)
    }

    private fun createFeatureSets(name: String, ranges: List<Range>): Array<BaseVector<*, *, *>> {
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
        return vectors.toTypedArray()
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
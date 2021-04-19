import smile.data.DataFrame
import smile.data.type.DataTypes
import smile.data.type.StructField
import smile.data.vector.BaseVector
import smile.data.vector.DoubleVector

class BooleanDataFrame(input: DataFrame) {
    data class Range(
        var lower: Double,
        var upper: Double,
        val mean: Double,
        val std: Double
    )

    data class BooleanFeatureSet(
        val name: String,
        val set: Map<String, Pair<Double, Double>>
    )

    var dataframe: DataFrame
    val featureSets: MutableSet<BooleanFeatureSet>

    init {
        this.dataframe = input
        this.featureSets = mutableSetOf()
        this.toBooleanFeatures()
    }

    fun toBooleanFeatures() {
        val output: DataFrame = this.dataframe.outputs()
        val input = this.dataframe.inputs()

        val newCols = mutableSetOf<DataFrame>()

        input.schema().fields()
            .filter { it.isNumeric }
            .forEach {
                newCols.add(DataFrame.of(
                    *this.splitFeature(it.name)
                ) )
            }

        newCols.forEachIndexed { i, it ->
            this.dataframe = if (i == 0) it else this.dataframe.merge(it)
        }
        input.schema().fields()
            .filterNot { it.isNumeric }
            .forEach {
                this.dataframe = this.dataframe.merge(
                    DataFrame.of(input.column(it.name))
                )
            }
        this.dataframe = this.dataframe.merge(output)
    }

    private fun splitFeature(name: String): Array<BaseVector<*, *, *>> {
        fun Double.format(digits: Int) = "%.${digits}f".format(this)

        val ranges = this.createRanges(
            this.dataframe.select(name, this.dataframe.column(this.dataframe.lastColumnIndex).name()),
            name
        )
        val vectors: ArrayList<BaseVector<*, *, *>> = arrayListOf()

        BooleanFeatureSet(name, mapOf(
            *ranges.map {
                "$name ${it.lower.format(2)}-${it.upper.format(2)}" to
                    Pair(it.lower, it.upper)
            }.toTypedArray()
        ) ).apply {
            featureSets.add(this)
            this.set.forEach { k, v ->
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
            Range(desc!!.mean, desc.mean, desc.mean, desc.std)
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
package smile.feature

import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType

class InvertibleScaler(schema: StructType?, lo: DoubleArray?, hi: DoubleArray?) : Scaler(schema, lo, hi), InvertibleFeatureTransform {
    val lowerBounds: DoubleArray
        get() = super.lo

    val upperBounds: DoubleArray
        get() = super.hi

    override fun invert(x: DoubleArray?): DoubleArray? {
        TODO("Not yet implemented")
    }

    override fun invert(data: Array<DoubleArray?>): Array<DoubleArray?>? {
        TODO("Not yet implemented")
    }

    override fun invert(x: Tuple?): Tuple? {
        TODO("Not yet implemented")
    }

    override fun invert(data: DataFrame?): DataFrame? {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun fit(data: DataFrame): InvertibleScaler {
            require(!data.isEmpty) { "Empty data frame" }
            val schema = data.schema()
            val lo = DoubleArray(schema.length())
            val hi = DoubleArray(schema.length())
            for (i in lo.indices) {
                if (schema.field(i).isNumeric) {
                    lo[i] = data.doubleVector(i).stream().min().asDouble
                    hi[i] = data.doubleVector(i).stream().max().asDouble
                }
            }
            return InvertibleScaler(schema, lo, hi)
        }

        @JvmStatic
        fun fit(data: Array<DoubleArray>): InvertibleScaler = fit(DataFrame.of(data))
    }
}
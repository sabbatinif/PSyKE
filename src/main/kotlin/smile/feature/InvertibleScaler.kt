package smile.feature

import smile.data.DataFrame
import smile.data.type.StructType

internal class InvertibleScaler(schema: StructType, lo: DoubleArray, hi: DoubleArray) :
    Scaler(schema, lo, hi), InvertibleFeatureTransform {

    override val schema: StructType?
        get() = super.schema

    val lowerBounds: DoubleArray
        get() = super.lo

    val upperBounds: DoubleArray
        get() = super.hi

    override fun invert(x: Double, i: Int): Double {
        return this.upperBounds[i] * x + this.lowerBounds[i]
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

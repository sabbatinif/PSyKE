package smile.feature

import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType

internal class InvertibleScaler(schema: StructType, lo: DoubleArray, hi: DoubleArray) : Scaler(schema, lo, hi), InvertibleFeatureTransform {
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
            val scaler = Scaler.fit(data)
            return InvertibleScaler(scaler.schema, scaler.lo, scaler.hi)
        }

        @JvmStatic
        fun fit(data: Array<DoubleArray>): InvertibleScaler = fit(DataFrame.of(data))
    }
}
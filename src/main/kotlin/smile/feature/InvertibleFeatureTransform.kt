package smile.feature

import smile.data.DataFrame
import smile.data.Tuple
import smile.data.type.StructType

interface InvertibleFeatureTransform : FeatureTransform {

    fun invert(x: DoubleArray?): DoubleArray?

    fun invert(data: Array<DoubleArray?>): Array<DoubleArray?>?

    fun invert(x: Tuple?): Tuple?

    fun invert(data: DataFrame?): DataFrame?

    companion object {
        fun scaler(schema: StructType, lo: DoubleArray, hi: DoubleArray): InvertibleFeatureTransform =
            InvertibleScaler(schema, lo, hi)

        fun scaler(data: DataFrame): InvertibleFeatureTransform = InvertibleScaler.fit(data)

        fun scaler(data: Array<DoubleArray>): InvertibleFeatureTransform = InvertibleScaler.fit(data)

        fun standardiser(data: DataFrame): InvertibleFeatureTransform = TODO()
    }
}
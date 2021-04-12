package smile.feature

import smile.data.DataFrame
import smile.data.Tuple
import smile.feature.FeatureTransform

interface InvertibleFeatureTransform : FeatureTransform {

    fun invert(x: DoubleArray?): DoubleArray?

    fun invert(data: Array<DoubleArray?>): Array<DoubleArray?>?

    fun invert(x: Tuple?): Tuple?

    fun invert(data: DataFrame?): DataFrame?
}
import it.unibo.tuprolog.theory.Theory
import smile.data.DataFrame
import java.util.function.ToDoubleFunction

interface Extractor<T, F : ToDoubleFunction<T>> {
    val predictor: F

    val dataset: DataFrame

    fun extract(): Theory
}
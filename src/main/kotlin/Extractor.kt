import it.unibo.tuprolog.theory.Theory
import java.util.function.ToDoubleFunction

interface Extractor<T, F : ToDoubleFunction<T>> {
    val predictor: F

    val input: Iterable<T>
    val expected: Iterable<T>

    fun extract(): Theory
}
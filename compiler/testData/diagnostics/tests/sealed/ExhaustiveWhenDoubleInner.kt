sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
        // It's ALLOWED to instantiate Sealed also here
        object Fourth: Sealed(78)
    }    
}

fun foo(s: Sealed) = when(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Fourth -> 4
    // no else required
}

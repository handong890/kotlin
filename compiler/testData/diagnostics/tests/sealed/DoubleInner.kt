sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
        // It's not allowed to instantiate Sealed here
        object Fourth: <!FINAL_SUPERTYPE!>Sealed<!>(78)
    }
}

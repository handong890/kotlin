sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
        fun foo(): Int {
            // It's not allowed to instantiate Sealed here
            val s = object: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Sealed<!>(78) {}
            return s.hashCode()
        }
    }
}

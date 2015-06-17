sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    sealed class NonFirst(val x: Int, val y: Int) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
        object Fourth: Sealed(78)
    }    
}

fun foo(s: Sealed, nf: Sealed.NonFirst): Int {
    val si = when(s) {
        Sealed.First -> 1
        Sealed.NonFirst.Fourth -> 4
    }
    val nfi = when(nf) {
        Sealed.NonFirst.Second -> 2
        Sealed.NonFirst.Third -> 3
    }
    return si + nfi
}

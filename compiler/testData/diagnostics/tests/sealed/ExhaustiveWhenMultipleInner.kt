sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        class NonSecond(x: Int, y: Int, val z: Int): NonFirst(x, y) {
            object Third: Sealed(56)
            class NonThird(x: Int): Sealed(x) {
                object Fourth: NonFirst(78, 90)
                class Fifth(x: Int): Sealed(x)
            }    
        }
        object Second: Sealed(34)
    }    
}

fun foo(s: Sealed) = when(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Second -> 4
    Sealed.NonFirst.NonSecond.Third -> 6
    is Sealed.NonFirst.NonSecond.NonThird -> 8
    is Sealed.NonFirst.NonSecond.NonThird.Fifth -> 10
    // no else required
}

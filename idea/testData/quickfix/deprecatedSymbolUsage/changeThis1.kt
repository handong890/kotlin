// "Replace with 's.newFun(this)'" "true"

class X {
    @deprecated("", ReplaceWith("s.newFun(this)"))
    fun oldFun(s: String){}
}

fun String.newFun(x: X){}

fun foo(x: X) {
    x.<caret>oldFun("a")
}

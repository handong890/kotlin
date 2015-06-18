public sealed class Season {
    class Warm: Season()
    class Cold: Season()
}

// TODO: Fix / delete this test, error should be here
fun foo(): Season = Instance()

fun box() = when(foo()) {
    is Season.Warm -> "Fail: Warm"
    is Season.Cold -> "Fail: Cold"
    else -> "Fail: Dead else activated"
}

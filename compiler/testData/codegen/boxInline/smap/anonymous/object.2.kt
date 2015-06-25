package builders

inline fun call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) init: () -> Unit) {
    return object {
        fun run () {
            init()
        }
    }.run()
}


//SMAP
//object.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object.2.kt
//builders/object_2$call$1
//*L
//1#1,21:1
//*E
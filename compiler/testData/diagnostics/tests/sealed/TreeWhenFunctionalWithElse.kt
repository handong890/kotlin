sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = when(this) {
        is Leaf  -> <!DEBUG_INFO_SMARTCAST!>this<!>.x
        is Node  -> <!DEBUG_INFO_SMARTCAST!>this<!>.left.max()
        else -> -1
    }
}

sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = <!NO_ELSE_IN_WHEN!>when<!>(this) {
        Empty -> -1
        <!NO_COMPANION_OBJECT!>Leaf<!>  -> 0
        is Node  -> <!DEBUG_INFO_SMARTCAST!>this<!>.left.max()
    }
}

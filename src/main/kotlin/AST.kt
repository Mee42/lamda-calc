package dev.mee42


class AST(val expr: Expr)

sealed class Expr {

    object UnitExpr: Expr() {
        override fun toString() = "()"
    }
    data class Lambda(val inputVariable: String, val body: Expr): Expr() {
        override fun toString() = "(\\$inputVariable -> $body)"
    }
    data class Application(val left: Expr, val right: Expr): Expr() {
        override fun toString() = "($left) ($right)"
    }
    data class Thunk(val name: String): Expr() {
        override fun toString() = name
    }
    data class Tuple(val left: Expr, val right: Expr): Expr() {
        override fun toString() = "($left, $right)"
    }
    data class ScopedExpr(val bindingName: String, val bindingExpr: Expr, val body: Expr): Expr() {
        override fun toString() = "($bindingName = $bindingExpr |- $body)"
    }
}

package dev.mee42



typealias MatchableExpr = Expr


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

    data class IntExpr(val i: Int): Expr() {
        override fun toString() = "$i"
    }
    data class MatchExpr(val expr: Expr, val cases: AssocList<MatchableExpr, Expr>): Expr() {
        override fun toString() = "match ($expr) " + cases.joinToString(" ","{ ", " }") { (a, b) -> "$a -> $b;" }
    }
}

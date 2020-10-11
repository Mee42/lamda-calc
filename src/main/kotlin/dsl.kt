package dev.mee42

infix fun Expr.ap(other: Expr): Expr {
    return Expr.Application(this, other)
}
infix fun Expr.tuple(b: Expr): Expr = Expr.Tuple(this, b)
@JvmName("tuple1")
fun tuple(a: Expr, b: Expr): Expr = a.tuple(b)

fun String.t() = Expr.Thunk(this)
fun l(name: String, body: Expr) = Expr.Lambda(name, body)
fun l(name: String, body: (Expr) -> Expr) = Expr.Lambda(name, body(name.t()))
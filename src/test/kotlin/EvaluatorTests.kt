package dev.mee42
import org.junit.Test
import org.junit.Assert.*
import kotlin.reflect.KProperty

infix fun <T> T.assertEq(other: T) {
    assertEquals(other, this)
}
fun eval(expr: Expr, environment: Environment = Environment.nil, weak: Boolean = false): Expr =
        ::trimOutUnusedScopeContexts `$`
                 if (weak) evalWeakHead(expr, environment) else evalNormalForm(expr, environment)


class EnvironmentCreator {
    var env = Environment.nil
    inner class Bind(private val value: Expr) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Expr.Thunk {
            if(!env.thunks.containsKey(property.name))
                env = env.with(property.name, value)
            return Expr.Thunk(property.name)
        }
    }
    fun bind(expr: Expr): Bind {
        return Bind(expr)
    }
    
    fun eval(expr: Expr, weak: Boolean = false): Expr {
        return eval(expr, env, weak)
    }
}

fun env(scope: EnvironmentCreator.() -> Unit) {
    EnvironmentCreator().apply(scope)
}


inline fun <reified T: Throwable> shouldThrow(block: () -> Unit): T {
    try {
        block()
    } catch(e: Throwable) {
        if(e is T) {
            return e
        } else throw e
    }
    fail("Should have throw ${T::class.simpleName}, but didn't")
    error("fail should have returned, but didn't")
}
val unit = Expr.UnitExpr

class EvaluatorTests {
    @Test
    fun `unit value is constant`() {
        eval(unit) assertEq unit
    }
    @Test
    fun `tuple values are constant`(){
        eval(tuple(unit, unit)) assertEq tuple(unit, unit)
    }
    @Test
    fun `tuple values don't eval inner in WHNF`(){
        val x = tuple(l("x") { it } ap unit, unit)
        eval(x, weak = true) assertEq x
    }
    @Test
    fun `application does evaluate when asked for in WHNF`(){
        val x = l("x") { it } ap unit
        eval(x, weak = true) assertEq unit
    }
    @Test
    fun `id function on lambda`(){
        val x = l("x") { unit }
        val id = l("id") { it }
        eval(id ap x) assertEq x
    }
    @Test
    fun `self tuple function`(){
        eval(l("x") { tuple(it, it) } ap unit) assertEq tuple(unit, unit)
    }
    @Test
    fun `tuples of a variable do not evaluate the variable with WHNF`(){
        env {
            val x by bind(unit)
            eval(tuple(x, x), weak = true) assertEq tuple(x, x)
            eval(tuple(x, x)) assertEq tuple(unit, unit)
        }
    }
    @Test
    fun `multiple thunk evaluation`(){
        env {
            val a by bind(unit)
            val b by bind(l("it") { it })
            val c by bind(b)
            val d by bind(tuple(a, c))
            eval(tuple(d, c), weak = true) assertEq tuple(d, c)
            eval(tuple(d, c)) assertEq tuple(tuple(unit, l("it") { it }), l("it") { it })
        }
    }
    @Test
    fun `weak head doesn't evaluate thunk`(){
        shouldThrow<IllegalStateException> {
            eval("a".t(), weak = true)
        }.message assertEq "Thunk a does not exist in context {}"
        Pretty.reset() // otherwise the rest of the indentation gets screwed up
    }
    @Test
    fun `recursive id evaluation`(){
        env {
            val id by bind(l("a") { it })
            val id2 = id ap id
            eval(id2 ap tuple(unit, id), weak = true) assertEq tuple(unit, id)
            eval(id2 ap tuple(unit, id)) assertEq tuple(unit, l("a") { it })
        }
    }
    @Test
    fun `collapse scopings into scopeable applier`(){
        val expr = Expr.ScopedExpr(
                "a", tuple(unit, unit),
                body = Expr.ScopedExpr(
                        "b", Expr.Thunk("a"),
                        body = tuple("a".t(), "b".t())
                )
        )
        val (collapsed, func) = collapseScopingsIntoScopingApplier(expr)
        collapsed assertEq tuple("a".t(), "b".t())
        func(collapsed) assertEq expr

        func(
                Expr.Lambda("c", collapsed)
        ) assertEq Expr.ScopedExpr("a", tuple(unit, unit), body =
            Expr.ScopedExpr("b", Expr.Thunk("a"), body = 
                Expr.Lambda("c", tuple("a".t(), "b".t()))
            )
        )
    }
}

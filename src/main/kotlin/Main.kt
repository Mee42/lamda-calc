package dev.mee42



class Environment(val thunks: Map<String, Expr>) {
    companion object { 
        val nil = Environment(emptyMap())
    }
    fun with(name: String, expr: Expr): Environment {
        return Environment(thunks + mapOf(name to expr))
    }

    override fun toString(): String {
        return thunks.entries.joinToString(", ","{","}") { (k,v) -> "$k:$v" }
    }
}

/**
 * this takes into an expression 'expr', which can be anything
 * and then returns a pair of (ret, func)
 *  ret is an expression that is *NOT* ScopedExpr
 *  func is a function that will take in an Expr and 'add back' the scoped variables
 *
 * guarantees:
 *  func(ret) == expr
 *  forall x , forall expr where expr != ScopeExpr, func(x) == x
*/
fun collapseScopingsIntoScopingApplier(expr_: Expr): Pair<Expr, (Expr) -> Expr> {
    val scopings = mutableListOf<Pair<String, Expr>>()
    var expr = expr_
    while(expr is Expr.ScopedExpr) {
        scopings.add(expr.bindingName to expr.bindingExpr)
        expr = expr.body
    }
    scopings.reverse()
    val func = { inputExpr: Expr ->
        var input = inputExpr
        for((bindingName, bindingExpr) in scopings) {
            input = Expr.ScopedExpr(bindingName, bindingExpr, input)
        }
        input
    }
    return expr to func
}



// will only ever return: ScopedExpr, UnitExpr, Lambda, Tuple
fun evalWeakHead(expr: Expr, environment: Environment): Expr {
    Pretty.stepInto("eval whnf $environment ||- $expr")
    val ret = when(expr) {
        Expr.UnitExpr, is Expr.Lambda, is Expr.Tuple -> expr
        is Expr.Application -> {
            val (left, reapplyScoping) = ::collapseScopingsIntoScopingApplier `$` evalWeakHead(expr.left, environment)
            left as? Expr.Lambda ?: error("not a lambda fuck you")
            evalWeakHead(reapplyScoping(Expr.ScopedExpr(left.inputVariable, expr.right, left.body)), environment)
        }
        is Expr.ScopedExpr -> {
            val e = evalWeakHead(expr.body, environment.with(expr.bindingName, expr.bindingExpr))
            Expr.ScopedExpr(expr.bindingName, expr.bindingExpr, e)
        }
        is Expr.Thunk -> {
           evalWeakHead(environment.thunks[expr.name] ?: error("Thunk ${expr.name} does not exist in context $environment"), environment)
        }
    }
    Pretty.stepOut("returning: $ret")
    return ret
}
fun visit(expr: Expr, f: (Expr) -> Expr?): Expr = f(expr) ?: when(expr){
    Expr.UnitExpr -> Expr.UnitExpr
    is Expr.Lambda -> Expr.Lambda(expr.inputVariable, visit(expr.body, f))
    is Expr.Application -> Expr.Application(visit(expr.left, f), visit(expr.right, f))
    is Expr.Thunk -> expr
    is Expr.Tuple -> Expr.Tuple(visit(expr.left, f), visit(expr.right, f))
    is Expr.ScopedExpr -> Expr.ScopedExpr(expr.bindingName, visit(expr.bindingExpr, f), visit(expr.body, f))
}


fun <T> reduceTwo(a: Expr, b: Expr, f: (Expr) -> T?, m: (T, T) -> T): T? = reduce(a, f, m)?.let { x -> reduce(b, f, m)?.let { y -> m(x, y) } }
fun <T> reduce(expr: Expr, f: (Expr) -> T?, m: (T, T) -> T): T? = f(expr) ?: when(expr) {
    Expr.UnitExpr, is Expr.Thunk -> null
    is Expr.Lambda -> reduce(expr.body, f, m)
    is Expr.Application -> reduceTwo(expr.left, expr.right, f, m)
    is Expr.Tuple -> reduceTwo(expr.left, expr.right, f, m)
    is Expr.ScopedExpr -> reduceTwo(expr.bindingExpr, expr.body, f, m)
}

fun usedVariables(expr: Expr): Set<String> =
        reduce(expr, f = { if(it is Expr.Thunk) LeafTree(it.name) else null }, m = ::MergeTree)?.copyToSet() ?: emptySet()

// TODO do we need this ever?
fun trimEnvironment(expr: Expr, environment: Environment): Environment {
    val usedVariables = usedVariables(expr)
    return ::Environment `$` environment.thunks.filter { (k ) -> k in usedVariables }
}
// we don't actually need to take in any environment because scoped contexts can't be used in environments higher then them
fun trimOutUnusedScopeContexts(expr: Expr): Expr {
    return visit(expr) {
        if(it !is Expr.ScopedExpr) return@visit null
        val usedVariables = usedVariables(it.body)
        if(it.bindingName in usedVariables) return@visit null
        else return@visit trimOutUnusedScopeContexts(it.body)
    }
}

// will return code in "normal form" - so it'll evaluate everything, basically
fun evalNormalForm(expr: Expr, environment: Environment): Expr {
    Pretty.stepInto("eval nf $environment ||- $expr")
    val ret =  when(expr) {
        Expr.UnitExpr -> Expr.UnitExpr
        is Expr.Lambda -> expr
        is Expr.Application -> evalNormalForm(evalWeakHead(expr, environment), environment)
        is Expr.Thunk -> evalNormalForm(environment.thunks[expr.name] ?: error("Thunk ${expr.name} does not exist in context $environment"), environment)
        is Expr.Tuple -> Expr.Tuple(evalNormalForm(expr.left, environment), evalNormalForm(expr.right, environment))
        is Expr.ScopedExpr -> {
            Expr.ScopedExpr(expr.bindingName, expr.bindingExpr, evalNormalForm(expr.body, environment.with(expr.bindingName, expr.bindingExpr)))
        }
    }
    Pretty.stepOut("returning: $ret")
    return ret
}



fun main() {
    val env = Environment.nil
            .with("id", l("it") { it })
    val ast = l("x") { it } ap tuple("id".t(), Expr.UnitExpr)

    Pretty.stepInto("WHNF Evaluation")
    val result = evalWeakHead(ast, env)
    Pretty.stepOut("Result: $result")
    println()
    Pretty.stepInto("NF Evaluation")
    val normal = trimOutUnusedScopeContexts(evalNormalForm(result, env))
    Pretty.stepOut("Result: $normal")
}
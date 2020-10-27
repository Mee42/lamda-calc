package dev.mee42


class Environment(val thunks: Map<String, Expr>) {
    companion object { 
        val nil = Environment(emptyMap())
    }
    fun with(name: String, expr: Expr): Environment {
        val e = Environment(thunks + mapOf(name to expr))
        return e
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
        Expr.UnitExpr, is Expr.Lambda, is Expr.Tuple, is Expr.IntExpr -> expr
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
        is Expr.MatchExpr -> {
            // ok, we gotta pick the branch
            var input = expr.expr
            var returnState: Expr? = null
            for((matchExpr, valueExpr) in expr.cases){
                val (newInput, result) = pickBranch(matchExpr, input, environment)
                if(result != null) {
                    var ret = valueExpr
                    for((thunk, e) in result.thunks)
                        ret = Expr.ScopedExpr(thunk, e, ret)
                    returnState = ret
                    break
                } else input = newInput
            }
            evalWeakHead(returnState ?: error("no match in case statement"), environment)
        } 
    }
    Pretty.stepOut("returning: $ret")
    return ret
}

data class MatchResult(val thunks: Map<String, Expr>)

fun pickBranch(matchExpr: MatchableExpr, expr: Expr, env: Environment): Pair<Expr, MatchResult?> {
    // ok, we gonna take out all the scoping rules
    when (matchExpr) {
        is Expr.Thunk -> {
            val (input, reapplyScope) = collapseScopingsIntoScopingApplier(expr)
            return reapplyScope(input) to MatchResult(mapOf(matchExpr.name to input))
        }
        is Expr.IntExpr -> {
            val (input, reapplyScope) = collapseScopingsIntoScopingApplier(evalWeakHead(expr, env))
            if (input !is Expr.IntExpr) return reapplyScope(input) to null
            // ok, so it's good
            return if(input.i == matchExpr.i) reapplyScope(input) to MatchResult(emptyMap())
            else reapplyScope(input) to null
        }
        is Expr.Tuple -> {
            val (input, reapplyScope) = collapseScopingsIntoScopingApplier(evalWeakHead(expr, env))
            if(input !is Expr.Tuple) return reapplyScope(input) to null
            val (leftExpr, rightExpr) = input
            val (leftMatchExpr, rightMatchExpr) = matchExpr

            val (leftMoreEvaluated, matchResultLeft) = pickBranch(leftMatchExpr, reapplyScope(leftExpr), env)
            if(matchResultLeft == null) {
                return Expr.Tuple(leftMoreEvaluated, reapplyScope(rightExpr)) to null
            }
            val (rightMoreEvaluated, matchResultRight) = pickBranch(rightMatchExpr, reapplyScope(rightExpr), env)
            if(matchResultRight == null) {
                return Expr.Tuple(leftMoreEvaluated, rightMoreEvaluated) to null
            }
            val newResults = MatchResult(matchResultLeft.thunks + matchResultRight.thunks)
            return Expr.Tuple(leftMoreEvaluated, rightMoreEvaluated) to newResults
        }
        Expr.UnitExpr -> TODO()
        is Expr.Lambda -> TODO()
        is Expr.Application -> TODO()
        is Expr.ScopedExpr -> TODO()
        is Expr.MatchExpr -> TODO()
    }
}

fun usedVariables(expr: Expr, s: Set<String>): Set<String> = when(expr) {
    Expr.UnitExpr -> emptySet()
    is Expr.Lambda -> {
        if(expr.inputVariable in s) emptySet() else usedVariables(expr.body, s)
    }
    is Expr.Application -> usedVariables(expr.left, s) + usedVariables(expr.right, s)
    is Expr.Thunk -> setOf(expr.name)
    is Expr.Tuple -> usedVariables(expr.left, s) + usedVariables(expr.right, s)
    is Expr.ScopedExpr -> {
        if(expr.bindingName in s) emptySet()
        else usedVariables(expr.bindingExpr, s) + usedVariables(expr.body, s)
    }
    is Expr.IntExpr -> emptySet()
    is Expr.MatchExpr -> usedVariables(expr.expr, s) + expr.cases.fold(emptySet()) {
            a, (match, e) -> // variables that are shadowed (contained in matchexpr) are not returned
                a + usedVariables(e, s).filter { x -> x !in  usedVariables(match, emptySet()) }
    }
}

// TODO do we ever need this? 
fun trimEnvironment(expr: Expr, environment: Environment): Environment {
    val usedVariables = usedVariables(expr, environment.thunks.map { it.key }.toSet())
    return ::Environment `$` environment.thunks.filter { (k ) -> k in usedVariables }
}
// we don't actually need to take in any environment because scoped contexts can't be used in environments higher then them
fun trimOutUnusedScopeContexts(expr: Expr): Expr = when (expr) {
    is Expr.ScopedExpr -> {
        val usedVariables = usedVariables(expr.body, setOf(expr.bindingName))
        if (expr.bindingName in usedVariables) {
            Expr.ScopedExpr(expr.bindingName, trimOutUnusedScopeContexts(expr.bindingExpr), trimOutUnusedScopeContexts(expr.body))
        } else trimOutUnusedScopeContexts(expr.body)
    }
    Expr.UnitExpr, is Expr.Thunk, is Expr.IntExpr -> expr
    is Expr.Lambda -> Expr.Lambda(expr.inputVariable, trimOutUnusedScopeContexts(expr.body))
    is Expr.Application -> Expr.Application(trimOutUnusedScopeContexts(expr.left), trimOutUnusedScopeContexts(expr.right))
    is Expr.Tuple -> Expr.Tuple(trimOutUnusedScopeContexts(expr.left), trimOutUnusedScopeContexts(expr.right))
    is Expr.MatchExpr -> {
        val main = trimOutUnusedScopeContexts(expr.expr)
        val cases = expr.cases.map { (matchExpr, valueExpr) ->
            matchExpr to trimOutUnusedScopeContexts(valueExpr)
        }
        Expr.MatchExpr(main, cases)
    }
}

// will return code in "normal form" - so it'll evaluate everything, basically
fun evalNormalForm(expr: Expr, environment: Environment): Expr {
    Pretty.stepInto("eval nf $environment ||- $expr")
    val ret =  when(expr) {
        is Expr.Lambda, is Expr.IntExpr, Expr.UnitExpr -> expr
        is Expr.Application -> evalNormalForm(evalWeakHead(expr, environment), environment)
        is Expr.Thunk -> evalNormalForm(environment.thunks[expr.name] ?: error("Thunk ${expr.name} does not exist in context $environment"), environment)
        is Expr.Tuple -> Expr.Tuple(evalNormalForm(expr.left, environment), evalNormalForm(expr.right, environment))
        is Expr.ScopedExpr -> {
            Expr.ScopedExpr(expr.bindingName, expr.bindingExpr, evalNormalForm(expr.body, environment.with(expr.bindingName, expr.bindingExpr)))
        }
        is Expr.MatchExpr -> TODO()
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

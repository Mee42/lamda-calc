package dev.mee42

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

sealed class Mode {
    object Eval: Mode()
    object Lex: Mode()
    object Parse: Mode()
    object EvalWeak: Mode()
    object ToggleDebug: Mode()
    class Bind(val name: String): Mode()
}


fun main(args: Array<String>) {


    val i = Scanner(System.`in`)
    var env = Environment.nil
    Pretty.on = false
    val filesReadIn = mutableListOf<String>()
    fun resetEnv() {
        env = Environment.nil
        for(file in filesReadIn) {
            env = load(File(file).readText(Charsets.UTF_8), env)
            println("loaded $file")
        }
    }
    filesReadIn += args
    resetEnv()
    while(true){
        try {
            print("λ ")
            val input = i.nextLine()
            val (mode, code) = (if (input.startsWith(":")) {
                if (input.length == 1) continue
                when(input[1]) {
                    'b' -> {
                        val (name, expr) = input.substring(2).trim().split(' ', limit = 2)
                        Mode.Bind(name) to expr
                    }
                    'l' -> {
                        val name = input.substring(2).trim()
                        val rest = File(name).readText(Charsets.UTF_8)
                        filesReadIn += name
                        env = load(rest, env)
                        null
                    }
                    'r' -> {
                        resetEnv()
                        null
                    }
                    else -> when (input[1]) {
                        'q' -> return
                        't' -> TODO("type")
                        'L' -> Mode.Lex
                        'w' -> Mode.EvalWeak
                        'p' -> Mode.Parse
                        'd' -> Mode.ToggleDebug
                        else -> error("unknown predicate ${input[1]}")
                    } to input.substring(2)
                }
            } else Mode.Eval to input) ?: continue
            if (input.isBlank()) continue
            when (mode) {
                Mode.Eval -> {
                    var x = evalWeakHead(parse(lex(code)) ?: error("can't do this "), env)
                    while(true) {
                        if(x is Expr.UnitExpr) break
                        else if (x is Expr.Tuple) {
                            val left = evalWeakHead(x.left, env)
                            if (left is Expr.IntExpr) {
                                print("$left ")
                                x = evalWeakHead(x.right, env)
                            } else error("can't print this as it is not a list")
                        } else error("can't print a non-tuple value")
                    }
                    println()
                }
                Mode.Lex -> {
                    println(lex(code))
                }
                Mode.Parse -> {
                    println(parse(lex(code)))
                }
                Mode.EvalWeak -> {
                    println(evalWeakHead(parse(lex(code))
                            ?: error("can't parse expression for some reason"), env))
                }
                is Mode.Bind -> {
                    val c = parse(lex(code)) ?: error("no expression")
                    println(c)
                    env = env.with(mode.name, c) // no need to do any sort of evaluation omegalul
                }
                Mode.ToggleDebug -> {
                    Pretty.on = !Pretty.on
                }
            }
        } catch (e: Throwable) {
            System.err.flush()
            System.out.flush()
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            System.err.println(sw.toString())
            Thread.sleep(10)
            System.err.flush()
            System.out.flush()
        }
    }
}


fun load(file: String, environment: Environment): Environment {
    val file2 = file.split("\n")
            .map { it.trim() }
            .map { if(it.contains("--")) it.split("--")[0] else it }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    val tokens = TokenQueue(lex(file2))
    var env = environment
    while(tokens.hasNext()) {
        val name = tokens.next().checkType(TokenType.IDENTIFIER).span
        tokens.next().checkType(TokenType.ASSIGN)
        val expr = parseExpr(tokens, greedy = true) ?: error("assignment without any value")
        tokens.next().checkType(TokenType.SEMICOLON)
        env = env.with(name, expr)
    }
    return env
}






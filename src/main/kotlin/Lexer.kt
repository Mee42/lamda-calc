package dev.mee42

import dev.mee42.TokenType.*
import java.util.regex.Pattern.compile

enum class TokenType { IDENTIFIER, NUMBER, LAMBDA, LPAREN, RPAREN, ARROW, COMMA, ASSIGN, SEMICOLON, LBRACE, RBRACE, KEYWORD_MATCH }

val matches = mapOf(
    compile("^match") to KEYWORD_MATCH,
    compile("""^[a-z]+""") to IDENTIFIER,
    compile("""^[0-9]+""") to NUMBER,
    compile("""^(\\)|(λ)""") to LAMBDA,
    compile("""^\(""") to LPAREN,
    compile("""^\)""") to RPAREN,
    compile("""^->""") to ARROW,
    compile("""^,""") to COMMA,
    compile("""^=""") to ASSIGN,
    compile("""^;""") to SEMICOLON,
    compile("""^\{""") to LBRACE,
    compile("""^}""") to RBRACE
)
data class Token(val span: String, val type: TokenType)

fun lex(str: String): List<Token> {
    var s = str.trimStart()
    val tokens = mutableListOf<Token>()
    loop@ while(s.isNotEmpty()) {
        for((pat, type) in matches) {
            val m = pat.matcher(s)
            if(m.find()) {
                val subMatch: String = m.group()
                tokens.add(Token(subMatch, type))
                s = s.substring(subMatch.length)
                s = s.trimStart()
                continue@loop
            }
        }
        error("Can't find token at '${s.substring(0, s.length.coerceAtMost(40))}'...")
    }
    return tokens
}
package dev.mee42

class TokenQueue(tokens: List<Token>){
    private val queue = ArrayDeque(tokens)
    fun next(): Token {
        if(queue.isEmpty()) error("token queue reached end")
        return queue.removeFirst()
    }
    fun hasNext() = queue.isNotEmpty()
    fun peek(): Token? = queue.firstOrNull()
}

fun parse(tokens: List<Token>): Expr? {
    return parseExpr(TokenQueue(tokens))
}
fun parseExpr(tokens: TokenQueue, repeat: Boolean = true): Expr? {
    val nextToken = tokens.next()
    var e = when(nextToken.type) {
        TokenType.LPAREN -> parseExpr(tokens).also { tokens.next().checkType(TokenType.RPAREN) }
        TokenType.NUMBER -> error("numbers not supported yet, sorry")
        TokenType.IDENTIFIER -> Expr.Thunk(nextToken.span)
        TokenType.LAMBDA -> {
            val name = tokens.next().checkType(TokenType.IDENTIFIER).span
            tokens.next().checkType(TokenType.ARROW)
            val body = parseExpr(tokens) ?: error("lambda has no body")
            Expr.Lambda(name, body)
        }
        TokenType.RPAREN -> null
        TokenType.ARROW -> error(":thinking:")
    } ?: return null
    if(tokens.peek()?.type in setOf(TokenType.RPAREN, null) || !repeat) {
        return e
    }
    while(tokens.peek()?.type !in setOf(TokenType.RPAREN, null)) {
        e = Expr.Application(e, parseExpr(tokens, repeat = false) ?: return e)
    }
    return e
}

private fun Token.checkType(expected: TokenType): Token {
    if(type != expected) error("expecting $expected, got $type on token '$span'")
    return this
}
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
// when repeat = false, it won't try and parse the next expression
fun parseExpr(tokens: TokenQueue, greedy: Boolean = true): Expr? {
    if(tokens.peek()?.type in listOf(TokenType.SEMICOLON, TokenType.RBRACE, TokenType.LBRACE, TokenType.ARROW)) return null
    val nextToken = tokens.next()
    var e = when(nextToken.type) {
        TokenType.LPAREN -> {
            if(tokens.peek()?.type == TokenType.RPAREN) {
                tokens.next() // we need to consume the )
                Expr.UnitExpr
            }
            else {
                val inner = parseExpr(tokens) ?: error("expecting expression")
                val ret = if(tokens.peek()?.type == TokenType.COMMA) {
                    tokens.next() // consume comma
                    val right = parseExpr(tokens) ?: error("floating comma")
                    Expr.Tuple(inner, right)
                } else inner
                tokens.next().checkType(TokenType.RPAREN)
                ret
            }
        }
        TokenType.NUMBER -> Expr.IntExpr(nextToken.span.toInt())
        TokenType.IDENTIFIER -> Expr.Thunk(nextToken.span)
        TokenType.LAMBDA -> {
            val name = tokens.next().checkType(TokenType.IDENTIFIER).span
            tokens.next().checkType(TokenType.ARROW)
            val body = parseExpr(tokens) ?: error("lambda has no body")
            Expr.Lambda(name, body)
        }
        TokenType.RPAREN -> null
        TokenType.COMMA, TokenType.ASSIGN -> error("can't start an expression with a \'" + nextToken.span + "'")
        TokenType.SEMICOLON, TokenType.LBRACE, TokenType.RBRACE, TokenType.ARROW  -> error("illegal compiler state")
        TokenType.KEYWORD_MATCH -> {
            /*
                    match <inputExpr> {
                        <matchExpr> -> <valueExpr>;
                        <matchExpr> -> <valueExpr>;
                    }
             */
            val inputExpr = parseExpr(tokens) ?: error("missing input expr in match expr")
            tokens.next().checkType(TokenType.LBRACE)
            val exprs = mutAssocListOf<MatchableExpr, Expr>()
            while(true) {
                if(tokens.peek()?.type == TokenType.RBRACE) { tokens.next(); break } // if there's a trailing semicolon
                val matchExpr = parseExpr(tokens) ?: error("missing expression in match expr")
                tokens.next().checkType(TokenType.ARROW)
                val valueExpr = parseExpr(tokens) ?: error("missing expression before semicolon in match expr")
                exprs.add(matchExpr to valueExpr)
                val next = tokens.next()
                if(next.type == TokenType.RBRACE) break
                next.checkType(TokenType.SEMICOLON)
            }
            Expr.MatchExpr(inputExpr, exprs)
        }
    } ?: return null
    if(!greedy) return e // if it's not greedy we'll just return, ezpz

    while(tokens.peek()?.type !in setOf(TokenType.RPAREN, TokenType.COMMA, null)) {
        e = Expr.Application(e, parseExpr(tokens, greedy = false) ?: return e)
    }
    return e
}

fun Token.checkType(expected: TokenType): Token {
    if(type != expected) error("expecting $expected, got $type on token '$span'")
    return this
}
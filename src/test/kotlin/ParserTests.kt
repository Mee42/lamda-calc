package dev.mee42

import org.junit.Test

class ParserTests {
    @Test
    fun `lexer tests`(){
        lex("""( ) \ foo bar 7 8 -> """) assertEq listOf(
                Token("(", TokenType.LPAREN),
                Token(")", TokenType.RPAREN),
                Token("\\", TokenType.LAMBDA),
                Token("foo", TokenType.IDENTIFIER),
                Token("bar", TokenType.IDENTIFIER),
                Token("7", TokenType.NUMBER),
                Token("8", TokenType.NUMBER),
                Token("->", TokenType.ARROW)
        )
    }
    @Test
    fun `parser parses basic function application`(){
        parse(lex("a b c"))
    }
}
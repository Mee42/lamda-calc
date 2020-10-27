package dev.mee42

import org.junit.Test

class ParserTests {
    @Test
    fun `lexer tests`(){
        lex("""( ) \ foo bar 7 8 -> (,)""") assertEq listOf(
                Token("(", TokenType.LPAREN),
                Token(")", TokenType.RPAREN),
                Token("\\", TokenType.LAMBDA),
                Token("foo", TokenType.IDENTIFIER),
                Token("bar", TokenType.IDENTIFIER),
                Token("7", TokenType.NUMBER),
                Token("8", TokenType.NUMBER),
                Token("->", TokenType.ARROW),
                Token("(", TokenType.LPAREN),
                Token(",", TokenType.COMMA),
                Token(")", TokenType.RPAREN)
        )
    }
    @Test
    fun `parser parses basic function application`(){
        parse(lex("a b c")) assertEq ((t("a") ap t("b")) ap t("c"))
    }
    @Test
    fun `parses application of lambda`(){
        parse(lex("""foo \x -> y""")) assertEq ("foo".t() ap l("x") { "y".t() })
    }
    @Test
    fun `parentheses work for application`(){
        parse(lex("a (b c)")) assertEq (t("a") ap (t("b") ap t("c")))
    }
    @Test
    fun `map function example`(){
        parse(lex("""
           (\x -> \y -> x) 
        """)) assertEq l("x") { l("y") { _ -> it } }
    }
    @Test
    fun `tuple syntax`(){
        parse(lex("(a, b)")) assertEq tuple(t("a"), t("b"))
    }
    @Test
    fun `3ary tuples not allowed`(){
        shouldThrow<IllegalStateException> {
            parse(lex("(a, b, c))")) assertEq Unit
        }.message assertEq "expecting RPAREN, got COMMA on token ','"
    }
    @Test
    fun `nested tuples are allowed`(){
        parse(lex("(a, (b, c))")) assertEq tuple(t("a"), tuple(t("b"),t("c")))
    }
    @Test
    fun `tuple with unit and thunk`(){
        lex("((), bar)") assertEq listOf(
                Token("(", TokenType.LPAREN),
                Token("(", TokenType.LPAREN),
                Token(")", TokenType.RPAREN),
                Token(",", TokenType.COMMA),
                Token("bar", TokenType.IDENTIFIER),
                Token(")", TokenType.RPAREN)
        )
        parse(lex("((), bar)")) assertEq tuple(unit, t("bar"))
    }
}
package dev.mee42

infix fun <A, B> ((A) -> B).`$`(v: A): B = this(v)
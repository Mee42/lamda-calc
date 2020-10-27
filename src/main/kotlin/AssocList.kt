package dev.mee42

typealias AssocList<A, B> = List<Pair<A, B>>

typealias MutAssocList<A, B> = MutableList<Pair<A, B>>

fun <A, B> mutAssocListOf(): MutAssocList<A,B> = mutableListOf()
package dev.mee42

object Pretty {
    var on = true
    var indentation = 0
    fun stepInto(str: String, increase: Int = 2) {
        if(on) println(" ".repeat(indentation) + ">".repeat(increase) + " " + str)
        indentation+=increase
    }
    fun stepOut(str: String? = null, decrease: Int = 2) {
        if(on && str != null) println(" ".repeat(indentation - decrease) + "<".repeat(decrease) + " " + str)
        indentation -= decrease
    }
    fun log(str: String) {
        if(on) println(" ".repeat(indentation)  + str)
    }
    fun reset() {
        indentation = 0
    }
}
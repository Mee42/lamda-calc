package dev.mee42

interface ConstantTree<T> {
    val size: Int
    fun iterator(): Iterator<T>
    fun copyToSet(): Set<T> {
        val set = HashSet<T>(size)
        for(elem in this.iterator()) {
            set.add(elem)
        }
        return set
    }
} 

class EmptyTree<T> : ConstantTree<T> {
    override val size: Int
        get() = 0

    override fun iterator(): Iterator<T> = emptySet<T>().iterator()
}

class LeafTree<T>(private val element: T) : ConstantTree<T> {
    override val size: Int = 1

    
    override fun iterator(): Iterator<T> = object: Iterator<T> {
        var consumed = false
        override fun hasNext(): Boolean = !consumed

        override fun next(): T = if(consumed) error("no") else element
    }
}
class MergeTree<T>(private val left: ConstantTree<T>, private val right: ConstantTree<T>): ConstantTree<T> {
    override val size: Int = left.size + right.size


    override fun iterator(): Iterator<T> = object: Iterator<T> {
        val a = left.iterator()
        val b = left.iterator()
        override fun hasNext(): Boolean = a.hasNext() || b.hasNext()
        override fun next(): T = if(a.hasNext()) a.next() else b.next()

    }

}
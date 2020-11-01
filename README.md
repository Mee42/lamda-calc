# lamda-calc

A simple interperter that's similar to haskell/stlc. Lazily evaluated. No IO at the moment.

Hello world program:

```hs
lls = \x -> ('l', ('l', x))
hello = \x -> ('h', ('e', lls ('o', x)))
world = ('w', ('o', ('r', ('l', ('d', Nil)))))

helloWorld = hello world
```

# Word Generator

*The current version of this project is coded in Java 17.
If you use this in your projects, please give me credit by linking this page!*

A word generator that generates random words from a given pattern. It can be used to generate phonetic words for conlangs, convoluted passwords, or original nicknames. This design is heavily based on [Awkwords](http://akana.conlang.org/tools/awkwords/) by Petr Mejzlik, but has some notable differences and is optimized for performance.

## Concepts

*For a more in-depth guide of the original concepts and syntax, see [here](http://akana.conlang.org/tools/awkwords/help.html).*

This generator makes use of a **main pattern**, which is used to construct the word, and up to 26 **subpatterns**, which are represented by capital letters and can be inserted in any place in the main pattern or other subpatterns for ease of use.

### Syntax

- **Lowercase letters** represent text.
- **Capital letters** refer to subpatterns. To type capital letters as text, *escape* them as described below.
- **Slashes** (`/`) divide text into options, and select one at random. E.g. `a/b/c` generates `a`, `b`, or `c` with equal chance.
- Text inside **brackets** (`[]`) encloses an expression. E.g. `a[b/c/d]` generates `ab`, `ac`, or `ad` with equal chance.
- Text inside **parentheses** (`()`) encloses an *optional* expression, similar to brackets. E.g. `a(b/c/d)` generates `a`, `ab`, `ac`, or `ad` with equal chance.
- You can assign **weights** to any option by appending `*x` to it, where `x` is a positive integer between 1 and 128. For example, `a/d*3` would be 3 times as likely to generate `d` as it would `a`. Items that do not have an explicit weight default to a weight of 1.
- **Double quotes** (`""`) escape all character within them, letting you write any character as literal text.

### Differences from Awkwords

This tool can essentially be used in the same way as Awkwords. It may be beneficial to design the patterns in Awkwords due to the web-based GUI, and then transfer them to this program. However, some key differences from the original Awkwords are:

- Pattern filtering is not supported.
- Optional text (surrounded in parentheses) use weighted probability to determine if they appear, instead of having a simple 50% chance. For example, in the original Awkwords `a(b/c)` would have an equal chance of generating `a` or any of `ab` or `ac`. In my version, this would generate `a`, `ab`, or `ac` with equal chance.

## Development

I originally wrote this generator for a personal project that dealt with generating phonetic names. I had already encountered Awkwords in the past, but needed a Java version to be compatible with the rest of my project.

The initial version of this generator simply transposed Awkword's [PHP code](https://github.com/nai888/awkwords/blob/master/core.php) to Java. However, over the course of a few years it went under several changes (mostly outside of this repository, hence why it doesn't show up in the commit log) as I tinkered more with it.

The main problem with the original implementation is that it is meant to be a tool first, and does not prioritize runtime performance. Every time a word would be generated, the program had to repeatedly parse the same string in a way that could not easily be cached, since random options were selected every time.

Recently, I designed a token-based implementation where during initialization, all patterns were parsed into a set of token objects, which could each be evaluated into a randomly generated string during runtime. This allowed me to define a reduction system that starts with the main pattern (which is now a token) and evaluates it recursively until a random string is formed. While I needed to rewrite nearly the entire algorithm to accomplish this, it was extremely worthwhile. Once the tokens were compiled, the runtime performance is almost **ten times faster** than the original string-based implementation.

It is good to note that storing these tokens uses far more memory than storing strings, and that there is a significant initial runtime penalty to parse the tokens. Since my project loaded a finite number of word generators at the start of the program and continually used them throughout the runtime, this approach was ideal for me.

I have attempted to make this as optimized as possible; for example, my program caches TokenLiterals to make sure that two tokens with the exact same string are not created. There are more modifications I have done on my own version of this generator to optimize it further, but I replaced them with more native Java code to decouple them from my project for this repository. If runtime performance is also important for you, I would recommend the following:

- Use [FastUtil](https://fastutil.di.unimi.it/) or an equivalent library for efficient primitive lists and maps. This avoids the runtime overhead of Java's wrapper classes and autoboxing.
- For better or more efficient RNG in [special cases](https://stackoverflow.com/questions/453479/how-good-is-java-util-random), use a different random number algorithm than `Math.random()`.
- This algorithm does not deal with subpatterns referencing other subpatterns since it parses them (mostly) in-order and requires the subpatterns it uses to already be parsed. One layer of reference should be fine, but any more than that may be unreliable. Circular references are, of course, not handled too well either.

## RandomCollection Data Structure

This project also makes use of a `RandomCollection` data structure, which is a Set-like data structure that allows you to insert items with a given weight, and efficiently generate weighted random choices. I have modified this class over the years from a [StackOverflow snippet](https://stackoverflow.com/a/6409791) and use it frequently; I'd highly recommend it if you're looking to implement a weighted random pool.

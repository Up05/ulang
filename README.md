# ulang
My small programming language, that, **one day**, may become a system-agnostic wrapper around scripting languages 

# Overview

## Comments

```Python
# Comments start with '#' and end with the line, just like in Python, Unix Shell or TOML. 
# I like it much more than '//' or '--' and I use ';' and '%' elsewhere
# I also do not believe that '/* */' is all that useful...
```

## Variables

Declaration: `<name> : <type> = <expression>`  
Assignment:  `<name> = <expression>`  
Use: `<name>`

E.g.:
```
a : num = 5
a = a + 2
println(a)
```
Unlike in most languages that use the `:=` operator, there is no type inference yet, 
so you cannot omit the type, i.e.: this is invalid: `a := 5`

## Types

```
num      a 64 bit floating point number, that, here, represents any number
string   Java's String type
bool     just a boolean.

[] <type>         Java's dynamic List<> wrapper.  
[] [] [] <type>   nesting arrays is supported.
```
*There are no integer types, since I do not yet have bitwise operators and possibility for cache coherency*
*Although, in case, I will want to interface with other languages, I have reserved Rust-like fixed-width int type names*

## Literals

Strings are in double quotes: `"`, characters *will be* in single quotes `'`   
Numbers are `<digits>[.<digits>]`, e.g.: `1`, `32767`, `1.0`, `5.0`...  
Bools are: `true` or `false`  
Arrays are: `[` followed by any number of elements and optional commas between them

## Comma omission
Commas are optional in function declaration, calling and arrays. 

I didn't really need commas for parsing at first and I like Nix's array syntax, 
so I left them as optional separators. Because of that you could have:
`['r' 'n' 't' 'b' 'a' 'v' '0']` or 
```
N : num = -0.5; P : num = 0.5
pts : [] num = [ N N N  N P P  P P N  P N P ]
```
*But be careful with commiting commas, since, what you may consider, a unary operator,
could in fact be a binary operator, take this for example: `[-1, -2  -3  -4]`
the resulting array would be: `[-1, -9]`*

## Operators

A table of all operators and their precedence.
```Ruby
Binary operators             Unary operators
 .                            + -
 * / %                        !
 + -
 == != < > <= >=
 && ||            # Btw, there is no short-circuiting
```
*Unary go before binary. Which, I just realized, !struct.a would be (!struct).a, 
but that's illogical either way, because, for example: I don't have structs...*

## Functions

Declaration: `func <name>(<arg>: <type>[ = <default value>]) [<return type>] {...}`,  
Calling: `<name>(<expr>)`

There is no C ABI or interfacing with Java, since I'm still unsure on whether I'll keep this
an interpreted language, transpile to C (or some other language) or (probably not) IR in the future.
And so, I have the most runtime-agnostic solution, which is just a couple of my own builtin functions:
```Go
func print(args: ..any)
func println(args: ..any)

 # More in "Errors" section
func handle(error: num)
func register_error(value: num  string: name) -> bool

func make_array(initial_length: num) -> [] any  # items 0..=initial_length are filled with `null`
func append(list: [] any, value: any)
func pop(list: [] any) any
func remove(list: [] any, index: num) any # decimals get truncated to int
func len(list: [] any) int
```

Example:
```Go
func array_contains_num(array: [] num  a: num) bool {
    for i: num = 0; i < len(array); i = i + 1 {
        if array[i] == a do return true
    }
    return false
}
```

## Loops 

Like in Go and Odin, there are only `for` loops here.

A normal for loop syntax looks like:
```
for [<declaration>]; [<boolean expression>]; [statement] { }
```
e.g.:
```
for i : num = 0; i < len(array); i = i + 1 {  }
```

However, for a while loop equivalent, simply, only provide a single expression:

```C
while(!glfwWindowShouldClose(window)) {  }
```
```
for !glfwWindowShouldClose(window) { }
```

By the way, there is no increment/decrement operators here, i.e.: `++` or `+=`, you have to do `i = i + 1`, 
I didn't think this was necessary to add, although I might eventually.

## Conditionals

There are currently only `if` statements, I want to add `switch` statements in the future.
And I, also, just simply forgot about `else` statements, oops...

Either way, `if` is just:
```
if <expr> { ... }
```

## Preprocessor

Currently, there is only a single preprocessor directive: `$insert "filename"`. 
It simply replaces itself with the contents of another file. 
You can do this ANYWHERE in the program. 

Although, generally, you should `$insert "std.u"` at the top of your file to include the standard library and 'error' variable

## Errors

I currently, do not have structures, maps, multiple return values, sum types and, importantly, threads, so, I believe, the simplest way to do errors for me, is to just have a global variable `error` of type integer.

You can register your own errors with the builtin `register_error(<error_val: int>, <name: string>)` function.  
The program can automatically handle an error for you with the builtin `handle(<error: int>)` function.

Errors higher than `0` are fatal, the program will exit if handle() is called with such an error passed through it.
Errors with value lower than `0` are warnings, I guess.
If `error` is 0, there is no error.



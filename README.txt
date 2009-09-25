Beak-Check - A type-testing library for Clojure.

This is a 'type predicate combinator' library that makes it easier
to write assertions about data structures in unit tests.

For instance, it makes it easy to check if a data structure
actually contains a set of 'persons'. The type checks can be as
'informal' or 'strict' as desired by the user.

A type predicate (TP) is a function that takes a data
structure as an argument and returns true if the data structure
complies to the type checked by the predicate and false otherwise.

Beak-Check is made of a few primitive TPs and some TP combinators
that allow the user to build complex type specifications (expressed
as TPs). It also uses existing Clojure TPs (such as set?, coll?,
nil? etc.) and adapts other functions that are very similar to a TP
(such as instance?).

The simplest TP is 'anything' which returns true regardless
of the data structure. It can be used in any combinator to show
that the type of a part of the data structure is irrelevant.

'sequence-of' (and friends) require that the data structure is a
certain kind of sequence (plain sequence, vector etc.) and that all
elements satisfy a type check predicate.

So (sequence-of anything) is a TP that only checks that the data
structure is a sequence of some sort. Examples:

user=> ((sequence-of anything) 1)
false
user=> ((sequence-of anything) '(1 2 3 nil))
true

'instance-of' wraps 'instance?' to make it look like a TP.

(vector-of (instance-of Integer))

will only match vectors of ints, not just any kind of
sequence. Examples:

user=> ((vector-of anything) [1 2 3])
true
user=> ((vector-of anything) '(1 2 3))
false
user=> ((vector-of (instance-of Integer)) [1 nil 3])
false

(nullable (instance-of Integer))

will match both nil and ints. Example:

user=> ((vector-of (nullable (instance-of Integer))) [1 nil 3])
true

Two basic combinators are 'check-all' and 'one-of' which are the
AND and OR of Beak-Check. 'check-all' requires that all of its
argument predicates match, and 'one-of' requirest that at least one
matches. These combinators are used to build 'vector-of' (and
friends), 'nullable' etc. Examples:

user=> ((one-of (instance-of Integer) (instance-of String)) 100)
true
user=> ((one-of (instance-of Integer) (instance-of String)) nil)
false
user=> ((one-of (instance-of Integer) (instance-of String)) "John")
true
user=> ((check-all (instance-of Integer) (partial < 10)) 100)
true
user=> ((check-all (instance-of Integer) (partial < 10)) 0)
false
user=> ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 2 3 4])
false
user=> ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 2])
true
user=> ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 nil])
false

(where 'fewer-than' is a TP that checks that the data structure is
a collection and has less than a given number of items).

There are more combinators that deal with structs and maps, see
below for examples.

Main caveat of Beak-Check: poor error reporting (TPs only return 'true' or
'false', probably the 'false' case should be more detailed).

More examples and tests are found in the source code.


;; Copyright (c) Miron Brezuleanu, 2009. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns beak-check.beak-check)

(defn anything
  "A primitive TP that matches anything."
  [data-structure]
  true)

(defn check-all
  "A TP combinator that requires that all <preds> return true."
  [& preds]
  (fn [data-structure]
    (reduce #(and %1 %2) true (map (fn [pred] (pred data-structure)) preds))))

(defn one-of [& preds]
  "A TP combinator that requires at least one of <preds> to return
  true."
  (fn [data-structure]
    (reduce #(or %1 %2) false (map (fn [pred] (pred data-structure)) preds))))

(defn sequence-of [pred]
  "A TP combinator that matches any kind of sequence with elements
  that match <pred>."
  (fn [data-structure]
    (if (coll? data-structure)
      (every? pred data-structure)
      false)))

(defn vector-of
  "A TP combinator that matches a vector of elements that match <pred>."
  [pred]
  (check-all vector? (sequence-of pred)))

(defn fewer-than
  "A primitive TP that matches a sequence with less than n elements."
  [n]
  (fn [data-structure]
    (if (coll? data-structure)
      (< (count data-structure) n)
      false)))

(defn more-than
  "A primitive TP that matches a sequence with more than n elements."
  [n]
  (fn [data-structure]
    (if (coll? data-structure)
      (> (count data-structure) n)
      false)))

(defn set-of
  "A TP combinator that matches a set of elements that match <pred>."
  [pred]
  (check-all set? (sequence-of pred)))

(defn map-of
  "A TP combinator that matches a map with values that match <pred>."
  [pred]
  (check-all map? (comp (sequence-of pred) vals)))

(defn nullable
  "A TP combinator that matches nil or whatever is matched by <pred>."
  [pred]
  (one-of nil? pred))

(defn instance-of
  "A primitive TP that matches an instance of <cls>."
  [cls]
  (partial instance? cls))

(defn has-only-keys
  "A primitive TP that matches maps that only have keys from
  <required-keys>."
  [& required-keys]
  (fn [data-structure]
    (if (instance? clojure.lang.IPersistentMap data-structure)
      (= (set (keys data-structure)) (set required-keys))
      false)))

(defn has-keys
  "A primitive TP that matches maps that have keys from
  <required-keys> and possibly other keys."
  [& required-keys]
  (fn [data-structure]
    (if (instance? clojure.lang.IPersistentMap data-structure)
      (let [required-keys-set (set required-keys)]
        (= (clojure.set/intersection (set (keys data-structure)) required-keys-set)
           required-keys-set))
      false)))

(defn- struct-test
  "A TP combinator helper that tests if the data structure is a struct
  constructed from <struct-base> and it also applies <key-test> to its
  keys."
  [struct-base key-test]
  (let [example-struct-keys (keys (struct struct-base))]
    (check-all (instance-of clojure.lang.PersistentStructMap)
               (apply key-test example-struct-keys))))

(defn like-struct
  "A primitive TP that matches data structures built from
  <struct-base> with possible additional keys."
  [struct-base]
  (struct-test struct-base has-keys))

(defn is-struct [struct-base]
  "A primitive TP that matches data structures built from
  <struct-base> but without any extra keys."
  (struct-test struct-base has-only-keys))

(defn with-values-for
  "A TP combinator that receives a list of keyword and TPs and matches
  maps that have keys with values matching the TPs. A struct walker
  building brick. Example:

  (with-values-for
    :name (nullable (instance-of String))
    :age (instance-of Integer))

  will match maps that have a :name key that points to either a nil or
  a String and an :age key that points to an int.

  Combine with 'is-struct' or 'like-struct' to build stricter struct
  checkers. See unit tests for examples."
  [& keys-preds]
  (let [keys-preds-partition (partition 2 keys-preds)
        the-keys (map first keys-preds-partition)
        the-preds (map second keys-preds-partition)
        make-test (fn [key pred] (fn [data-structure] (-> data-structure key pred)))]
    (apply check-all (map make-test the-keys the-preds))))



;; TODO
;; - write unit tests
;; - add docstrings
;;
;; Examples and tests:
;;
;; user=> ((map-of anything) {:a 1 :b 2})
;; true
;; user=> ((map-of (instance-of Integer)) {:a 1 :b 2})
;; true
;; user=> ((map-of (instance-of String)) {:a 1 :b 2})
;; false
;; user=> ((map-of (instance-of String)) {:a "John" :b "Mary"})
;; true
;; user=> ((set-of anything) [1 2 3])
;; false
;; user=> ((set-of anything) (set [1 2 3]))
;; true
;; user=> ((set-of (one-of (instance-of String) (instance-of Integer) nil?)) #{nil "John" 20})
;; true
;; user=> ((set-of (one-of (instance-of String) (instance-of Integer))) #{nil "John" 20 "Mary"})
;; false
;; user=> ((has-keys :name) {:name "John"})
;; true
;; user=> ((has-keys :name) 1)
;; false
;; user=> ((has-keys :name) [1 2 3])
;; false
;; user=> ((has-keys :name) {:aame "John"})
;; false
;; user=> ((has-keys :name) {:naame "John" :age 20})
;; false
;; user=> ((has-keys :name) {:name "John" :age 20})
;; true
;; user=> ((has-only-keys :name) {:name "John" :age 20})
;; false
;; user=> ((has-only-keys :name) {:name "John"})
;; true
;; user=> (defstruct <person> :name :age)
;; #'user/<person>
;; user=> ((has-keys :name) (struct <person>))
;; true
;; user=> ((is-struct <person>) 10)
;; false
;; user=> ((is-struct <person>) {:name "John" :age 20})
;; false
;; user=> ((is-struct <person>) (struct <person> "John" 20))
;; true
;; user=> ((is-struct <person>) (struct <person> 10 20))
;; true
;; user=> ((like-struct <person>) (struct-map <person> :name "John" :age 20 :other 1))
;; true
;; user=> ((like-struct <person>) #{1 2 3})
;; false
;; user=> ((like-struct <person>) {:name "John" :age 1})
;; false
;; user=> ;; nullable used just to show how it works
;; user=> (def is-person
;;             (check-all (is-struct <person>)
;;                        (with-values-for
;;                          :name (nullable (instance-of String))
;;                          :age (instance-of Integer))))
;; #'user/is-person
;; false
;; user=> (is-person (struct <person> "John" 20))
;; true
;; user=> (is-person (struct <person> nil 20))
;; true
;; user=> (is-person (struct <person> 10 20))
;; false

;; and a slightly more complex example to demonstrate composability
;; user=> (defstruct <address> :city :street :number)
;; #'user/<address>
;; user=> (defstruct <contact> :first-name :last-name :address)
;; #'user/<contact>
;; user=> (def is-address (check-all (is-struct <address>)
;;                                   (with-values-for
;;                                     :city (nullable (instance-of String))
;;                                     :street (instance-of String)
;;                                     :number (instance-of Integer))))
;; #'user/is-address
;; user=> (def is-contact (check-all (is-struct <contact>)
;;                                   (with-values-for
;;                                     :first-name (nullable (instance-of String))
;;                                     :last-name (instance-of String)
;;                                     :address is-address)))
;; #'user/is-contact
;; user=> (is-address 1)
;; false
;; user=> (is-address (struct <address> "City" "Street" "number"))
;; false
;; user=> (is-address (struct <address> "City" "Street" 100))
;; true
;; user=> (is-address (struct <address> nil "Street" 100))
;; true
;; user=> (is-address (struct <address> nil nil 100))
;; false
;; user=> (is-contact '(1 2 3))
;; false
;; user=> (is-contact (struct <contact> "John" "Smith" 100))
;; false
;; user=> (is-contact (struct <contact> "John" "Smith" (struct <address> nil "Street" 100)))
;; true
;; user=> (is-contact (struct <contact> "John" nil (struct <address> nil "Street" 100)))
;; false
;; user=> (is-contact (struct <contact> "John" "Smith" (struct <address> nil nil 100)))
;; false
;; ;; an alternative way to compose the checks for a contact.
;; ;; is-contact2 is equivalent to is-contact.
;; user=> (def is-contact2 (check-all (is-struct <contact>)
;;                                    (with-values-for
;;                                      :first-name (nullable (instance-of String))
;;                                      :last-name (instance-of String)
;;                                      :address (check-all (is-struct <address>)
;;                                                          (with-values-for
;;                                                            :city (nullable (instance-of String))
;;                                                            :street (instance-of String)
;;                                                            :number (instance-of Integer))))))
;; #'user/is-contact2
;; user=> (is-contact2 '(1 2 3))
;; false
;; user=> (is-contact2 (struct <contact> "John" "Smith" 100))
;; false
;; user=> (is-contact2 (struct <contact> "John" "Smith" (struct <address> nil "Street" 100)))
;; true
;; user=> (is-contact2 (struct <contact> "John" nil (struct <address> nil "Street" 100)))
;; false
;; user=> (is-contact2 (struct <contact> "John" "Smith" (struct <address> nil nil 100)))
;; false

(ns beak-check.beak-check)

(defn anything [data-structure]
  true)

(defn check-all [& preds]
  (fn [data-structure]
    (reduce #(and %1 %2) true (map (fn [pred] (pred data-structure)) preds))))

(defn one-of [& preds]
  (fn [data-structure]
    (reduce #(or %1 %2) false (map (fn [pred] (pred data-structure)) preds))))

(defn sequence-of [pred]
  (fn [data-structure]
    (if (coll? data-structure)
      (every? pred data-structure)
      false)))

(defn vector-of [pred]
  (check-all vector? (sequence-of pred)))

(defn fewer-than [n]
  (fn [data-structure]
    (if (coll? data-structure)
      (< (count data-structure) n)
      false)))

(defn more-than [n]
  (fn [data-structure]
    (if (coll? data-structure)
      (> (count data-structure) n)
      false)))

(defn set-of [pred]
  (check-all set? (sequence-of pred)))

(defn map-of [pred]
  (check-all map? (comp (sequence-of pred) vals)))

(defn nullable [pred]
  (one-of nil? pred))

(defn instance-of [cls]
  (partial instance? cls))

(defn has-only-keys [& required-keys]
  (fn [data-structure]
    (if (instance? clojure.lang.IPersistentMap data-structure)
      (= (set (keys data-structure)) (set required-keys))
      false)))

(defn has-keys [& required-keys]
  (fn [data-structure]
    (if (instance? clojure.lang.IPersistentMap data-structure)
      (let [required-keys-set (set required-keys)]
        (= (clojure.set/intersection (set (keys data-structure)) required-keys-set)
           required-keys-set))
      false)))

(defn- struct-test [struct-base key-test]
  (let [example-struct-keys (keys (struct struct-base))]
    (check-all (instance-of clojure.lang.PersistentStructMap)
               (apply key-test example-struct-keys))))

(defn like-struct [struct-base]
  (struct-test struct-base has-keys))

(defn is-struct [struct-base]
  (struct-test struct-base has-only-keys))

(defn with-values-for [& keys-preds]
  (let [keys-preds-partition (partition 2 keys-preds)
        the-keys (map first keys-preds-partition)
        the-preds (map second keys-preds-partition)
        make-test (fn [key pred] (fn [data-structure] (-> data-structure key pred)))]
    (apply check-all (map make-test the-keys the-preds))))


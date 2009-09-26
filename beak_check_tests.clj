
;; Copyright (c) Miron Brezuleanu, 2009. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns beak-check.beak-check-tests
  (:use beak-check.beak-check)
  (:use clojure.contrib.test-is))

(deftest test-anything
  (is (= true (anything 1)))
  (is (= true (anything nil)))
  (is (= true (anything [1 2 3]))))

(deftest test-sequence-of
  (is (= false ((sequence-of anything) 1)))
  (is (= true ((sequence-of anything) '(1 2 3 nil)))))

(deftest test-vector-of
  (is (= true ((vector-of anything) [1 2 3])))
  (is (= false ((vector-of anything) '(1 2 3))))
  (is (= false ((vector-of (instance-of Integer)) [1 nil 3])))
  (is (= true ((vector-of (nullable (instance-of Integer))) [1 nil 3]))))

(deftest test-one-of
  (is (= true ((one-of (instance-of Integer) (instance-of String)) 100)))
  (is (= false ((one-of (instance-of Integer) (instance-of String)) nil)))
  (is (= true ((one-of (instance-of Integer) (instance-of String)) "John"))))

(deftest test-check-all
  (is (= true ((check-all (instance-of Integer) (partial < 10)) 100)))
  (is (= false ((check-all (instance-of Integer) (partial < 10)) 0)))
  (is (= false ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 2 3 4])))
  (is (= true ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 2])))
  (is (= false ((check-all (vector-of (instance-of Integer)) (fewer-than 3)) [1 nil]))))

(deftest test-fewer-than
  (is (= true ((fewer-than 3) [1 2])))
  (is (= false ((fewer-than 3) [1 2 3 4 5]))))

(deftest test-more-than
  (is (= false ((more-than 3) [1 2])))
  (is (= true ((more-than 3) [1 2 3 4 5]))))

(defstruct <person> :name :age)

(deftest test-has-keys
  (is (= true ((has-keys :name) (struct <person>))))
  (is (= true ((has-keys :name) {:name "John"})))
  (is (= false ((has-keys :name) 1)))
  (is (= false ((has-keys :name) [1 2 3])))
  (is (= false ((has-keys :name) {:aame "John"})))
  (is (= false ((has-keys :name) {:naame "John" :age 20})))
  (is (= true ((has-keys :name) {:name "John" :age 20}))))

(deftest test-has-only-keys
  (is (= false ((has-only-keys :name) {:name "John" :age 20})))
  (is (= true ((has-only-keys :name) {:name "John"}))))

(deftest test-is-struct
  (is (= false ((is-struct <person>) 10)))
  (is (= false ((is-struct <person>) {:name "John" :age 20})))
  (is (= true ((is-struct <person>) (struct <person> "John" 20))))
  (is (= true ((is-struct <person>) (struct <person> 10 20)))))

(deftest test-like-struct
  (is (= true ((like-struct <person>) (struct-map <person> :name "John" :age 20 :other 1))))
  (is (= false ((like-struct <person>) #{1 2 3})))
  (is (= false ((like-struct <person>) {:name "John" :age 1}))))

(deftest test-with-values-for
  (let [is-person (check-all (is-struct <person>)
                             (with-values-for
                               :name (nullable (instance-of String))
                               :age (instance-of Integer)))]
    (is (= true (is-person (struct <person> "John" 20))))
    (is (= true (is-person (struct <person> nil 20))))
    (is (= false (is-person (struct <person> 10 20))))))

(deftest test-map-of
  (is (= true ((map-of anything) {:a 1 :b 2})))
  (is (= true ((map-of (instance-of Integer)) {:a 1 :b 2})))
  (is (= false ((map-of (instance-of String)) {:a 1 :b 2})))
  (is (= true ((map-of (instance-of String)) {:a "John" :b "Mary"}))))

(deftest test-set-of
  (is (= false ((set-of anything) [1 2 3])))
  (is (= true ((set-of anything) (set [1 2 3]))))
  (is (= true ((set-of (one-of (instance-of String)
                               (instance-of Integer)
                               nil?))
               #{nil "John" 20})))
  (is (= false ((set-of (one-of (instance-of String)
                                (instance-of Integer)))
                #{nil "John" 20 "Mary"}))))

(defstruct <address> :city :street :number)

(defstruct <contact> :first-name :last-name :address)

(deftest test-composing
  (let [is-address (check-all (is-struct <address>)
                              (with-values-for
                                :city (nullable (instance-of String))
                                :street (instance-of String)
                                :number (instance-of Integer)))
        is-contact (check-all (is-struct <contact>)
                              (with-values-for
                                :first-name (nullable (instance-of String))
                                :last-name (instance-of String)
                                :address is-address))
        is-contact2 (check-all (is-struct <contact>)
                               (with-values-for
                                 :first-name (nullable (instance-of String))
                                 :last-name (instance-of String)
                                 :address (check-all (is-struct <address>)
                                                     (with-values-for
                                                       :city (nullable (instance-of String))
                                                       :street (instance-of String)
                                                       :number (instance-of Integer)))))
        valid-contact (struct <contact>
                              "John"
                              "Smith"
                              (struct <address> nil "Street" 100))]
    (is (= false (is-address 1)))
    (is (= false (is-address (struct <address> "City" "Street" "number"))))
    (is (= true (is-address (struct <address> "City" "Street" 100))))
    (is (= true (is-address (struct <address> nil "Street" 100))))
    (is (= false (is-address (struct <address> nil nil 100))))
    (is (= false (is-contact '(1 2 3))))
    (is (= false (is-contact (struct <contact> "John" "Smith" 100))))
    (is (= true (is-contact (struct <contact>
                                    "John"
                                    "Smith"
                                    (struct <address> nil "Street" 100)))))
    (is (= false (is-contact (struct <contact>
                                     "John"
                                     nil
                                     (struct <address> nil "Street" 100)))))
    (is (= false (is-contact (struct <contact>
                                     "John"
                                     "Smith"
                                     (struct <address> nil nil 100)))))
    (is (= false (is-contact2 '(1 2 3))))
    (is (= false (is-contact2 (struct <contact> "John" "Smith" 100))))
    (is (= true (is-contact2 (struct <contact>
                                     "John"
                                     "Smith"
                                     (struct <address> nil "Street" 100)))))
    (is (= false (is-contact2 (struct <contact>
                                      "John"
                                      nil
                                      (struct <address> nil "Street" 100)))))
    (is (= false (is-contact2 (struct <contact>
                                      "John"
                                      "Smith"
                                      (struct <address> nil nil 100)))))
    (is (= true ((vector-of is-contact) [valid-contact valid-contact])))
    (is (= true ((vector-of (nullable is-contact)) [nil valid-contact valid-contact])))
    (is (= false ((vector-of is-contact) [nil valid-contact valid-contact])))))

(run-tests 'beak-check.beak-check-tests)

(ns pabloware.diff-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [pabloware.diff :as p :refer [diff patch]]))

(tabular
  (facts
    (diff ?a ?b) => ?delta
    (patch ?a ?delta) => ?b
    (patch ?a (diff ?a ?b)) => ?b)
  ?a ?b ?delta
  nil nil ::p/=
  {} {} ::p/=
  nil {} {}
  {} nil nil
  {:a :b} {:a :b} ::p/=
  {:a :b} {:a :B} {:a :B}
  {:a :b} {:a :b, :c :d} {:c :d}
  nil {:a :b} {:a :b}
  {} {:a :b} {:a :b}
  {:a :b, :c :d} {:a :b} {:c ::p/-}
  {:a :b} {} {}
  {:a :b} nil nil
  {:a :b, :c :d, :e :f} {:a :B, :c :d, :g :h} {:a :B, :e ::p/-, :g :h}
  {:a {:b :c, :d :e}} {:a {:b :C, :d :e}} {:a {:b :C}}
  {:a :b} {:a nil} {:a nil}
  [] [] ::p/=
  [] nil nil
  nil [] []
  [:a] [:a] ::p/=
  [:a] [:b] [:b]
  [:a :b] [:A :b] [:A ::p/=]
  [:a :b] [:a :B :C] [::p/= :B :C]
  [:a :b] [:a nil nil] [::p/= nil nil]
  [:a :b] [:a :b :c :d] [::p/+ :c :d]
  [] [:a :b] [:a :b]
  [:a :b] [:a] [::p/=]
  [{:a :b}] [{:a :b :c :d}] [{:c :d}]
  :a [:b] [:b]
  {0 0 1 0 2 0 3 0 4 0 5 0 6 0 7 0} {0 0 1 0 2 0 3 0 4 0 5 0 6 0 7 0 8 0} {8 0}
  [:a :b :c :d] [:A :b :c :D] [:A ::p/=*2 :D]
  #{:a} #{:a :b} #{:b}
  #{:a :b :c} #{:a :B :C} #{#{::p/- :b :c} :B :C}
  #{} #{nil} #{nil})

(tabular "list (not vector)"
  (facts
    (diff ?a ?b) => ?delta
    (diff ?a ?b) => list?
    (patch ?a ?delta) => ?b
    (patch ?a ?delta) => list?
    (patch ?a (diff ?a ?b)) => ?b)
  ?a ?b ?delta
  '(:a :b :c) '(:A :b) '(:A ::p/=)
  '(:a) '(:a :b) '(::p/+ :b)
  (list* [:a]) '(:a :b) '(::p/+ :b)
  [:a] '(:a :b) '(:a :b))

(s/def ::any (s/with-gen any? (constantly gen/any-equatable)))

(s/fdef p/diff
  :args (s/cat :a ::any :b ::any)
  :fn #(= (patch (-> % :args :a) (:ret %)) (-> % :args :b)))

(comment
  ;; 5000000 (five million) tests take one hour on Core i7
  (stest/check `diff {:clojure.spec.test.check/opts {:num-tests 5000000}}))

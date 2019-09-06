(ns pabloware.diff
  (:require [clojure.set :as set]))

(declare diff)

(defn- diff-map-from-b
  [a b]
  (reduce
    (fn [r k]
      (let [va (get a k)
            vb (get b k)]
        (if (= va vb)
          r
          (assoc r k (diff va vb)))))
    {}
    (keys b)))

(defn- diff-map-from-a
  [start a b]
  (reduce
    (fn [r k]
      (assoc r k ::-))
    start
    (set/difference (set (keys a)) (set (keys b)))))

(defn- diff-set
  [a b]
  (let [added (set/difference b a)
        removed (set/difference a b)]
    (if (seq removed)
      (conj added (conj removed ::-))
      added)))

(defn- diff-sequential-from-both
  [a b]
  (mapv
    (fn [va vb]
      (if (= va vb)
        ::=
        (diff va vb)))
    a
    b))

(defn- diff-sequential-from-b
  [start a b]
  (let [added (drop (count a) b)]
    (if (empty? added)
      start
      (into
        (if (and (seq start)
              (every? #(= % ::=) start))
          [::+]
          start)
        added))))

(def ^:private kw-ns (str *ns*))

(defn- =n
  [x]
  (and
    (keyword? x)
    (= (namespace x) kw-ns)
    (when-let [s (second (re-matches #"=\*(\d+)" (name x)))]
      (bigint s))))

(defn- compress-=-runs
  [start]
  (reduce
    (fn [r x]
      (cond
        (not= x ::=) (conj r x)
        (= (peek r) ::=) (conj (pop r) ::=*2)
        :else (if-let [n (=n (peek r))]
                (conj (pop r) (keyword kw-ns (str "=*" (inc n))))
                (conj r x))))
    []
    start))

(defn- decompress-=-runs
  [start]
  (reduce
    (fn [r x]
      (if-let [n (=n x)]
        (into r (repeat n ::=))
        (conj r x)))
    []
    start))

(defn- fix-sequential-type
  [start a]
  (if (vector? a)
    start
    (list* start)))

(defn- type'
  [x]
  (cond
    (map? x) :map
    (set? x) :set
    (vector? x) :vector
    (sequential? x) :list
    :else (type x)))

(defn diff
  [a b]
  (cond
    (= a b) ::=
    (and (seqable? b) (empty? b)) b
    (not= (type' a) (type' b)) b
    (map? a) (-> (diff-map-from-b a b)
               (diff-map-from-a a b))
    (set? a) (diff-set a b)
    (sequential? a) (-> (diff-sequential-from-both a b)
                      (diff-sequential-from-b a b)
                      (compress-=-runs)
                      (fix-sequential-type a))
    :else b))

(declare patch)

(defn- patch-map
  [a delta]
  (reduce-kv
    (fn [r k v]
      (if (= v ::-)
        (dissoc r k)
        (assoc r k (patch (get a k) v))))
    a
    delta))

(defn- patch-set-add
  [a delta]
  (set/union a delta))

(defn- patch-set-remove
  [start delta]
  (if-let [removed (first (filter ::- delta))]
    (apply disj start removed removed)
    start))

(defn- patch-sequential
  [a delta]
  (if (= (first delta) ::+)
    (into (vec a) (rest delta))
    (let [from-both (mapv
                      (fn [va vd]
                        (if (= vd ::=)
                          va
                          (patch va vd)))
                      a
                      delta)
          from-delta (drop (count a) delta)]
      (into from-both from-delta))))

(defn patch
  [a delta]
  (cond
    (= delta ::=) a
    (and (seqable? delta) (empty? delta)) delta
    (not= (type' a) (type' delta)) delta
    (map? delta) (patch-map a delta)
    (set? delta) (-> (patch-set-add a delta)
                   (patch-set-remove delta))
    (sequential? delta) (-> (patch-sequential a (decompress-=-runs delta))
                          (fix-sequential-type a))
    :else delta))

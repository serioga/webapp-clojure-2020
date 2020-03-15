(ns app.lib.util.perf
  "Replacement of core functions for better performance."
  #?(:clj (:import
            (clojure.lang Associative))))

#?(:clj (set! *warn-on-reflection* true) :cljs (set! *warn-on-infer* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))


(defn empty-coll?
  "Check if collection is empty using count if possible.
   Optimized for nil input."
  [coll]
  (cond
    (nil? coll) true
    (counted? coll) (zero? (count coll))
    :else (empty? coll)))


(defn not-empty-coll?
  "Check if collection is empty using count if possible.
   Optimized for nil input."
  [coll]
  (cond
    (nil? coll) false
    (counted? coll) (pos? (count coll))
    :else (seq coll)))


(defn fast-assoc*
  "Assoc using direct call to `Associative.assoc`."
  [m k v]
  (let [m (or m {})]
    #?(:clj (.assoc ^Associative m k v)
       :cljs (-assoc ^IAssociative m k v))))


(defmacro fast-assoc
  "Replacement for `clojure.core/assoc`.
   Multiple keys are assoc'ed as multiple calls to `fast-assoc*`."
  [m & kvs]
  (assert (even? (count kvs)))
  `(-> ~m
       ~@(map (fn [[k v]] (list 'app.lib.util.perf/fast-assoc* k v))
              (partition 2 kvs))))


(defn fast-merge*
  "Merge two hash-maps `m1` and `m2` using `fast-assoc*`.
   Check arguments if merge can be skipped ignoring nilable `m1` and empty `m2`."
  [m1 m2]
  (cond
    (nil? m1) m2
    (empty-coll? m2) m1
    :else (reduce-kv fast-assoc* m1 m2)))


(defmacro fast-merge
  "Replacement for `clojure.core/merge`.
   Multiple maps are merged with multiple calls to `fast-merge*`."
  [m & maps]
  `(-> ~m
       ~@(map #(list 'app.lib.util.perf/fast-merge* %) maps)))


(defmacro inline-dissoc
  "Call `clojure.core/dissoc` for multiple keys without loop over collection."
  [m & ks]
  `(-> ~m
       ~@(map (fn [k] (list 'clojure.core/dissoc k)) ks)))


(defn fast-select-keys
  "Alternative for `clojure.core/select-keys`.
   Ignores nullable values in map. Does not keep meta. Returns empty map for invalid args."
  [m ks]
  (cond
    (associative? m) (reduce (fn [acc k] (if-some [v (m k)]
                                           (fast-assoc* acc k v)
                                           acc))
                             {} ks)
    :else {}))


(comment
  (require '[criterium.core])

  (criterium.core/quick-bench
    (assoc {:a 1} :b 2))
  #_"Execution time mean : 39,736546 ns"

  (criterium.core/quick-bench
    (fast-assoc {:a 1} :b 2))
  #_"Execution time mean : 25,699835 ns, 35% faster"

  (criterium.core/quick-bench
    (pos? (count nil)))
  #_"Execution time mean : 2,960623 ns"

  (criterium.core/quick-bench
    (zero? (count nil)))
  #_"Execution time mean : 2,952321 ns"

  (criterium.core/quick-bench
    (zero? (count {})))
  #_"Execution time mean : 3,301519 ns"

  (criterium.core/quick-bench
    (not-empty {}))
  #_"Execution time mean : 10,164182 ns"

  (criterium.core/quick-bench
    (some? nil))
  #_"Execution time mean : 3,269397 ns"

  (criterium.core/quick-bench
    (nil? nil))
  #_"Execution time mean : 2,923687 ns"

  (criterium.core/quick-bench
    (not nil))
  #_"Execution time mean : 3,396421 ns"

  (macroexpand-1 '(fast-merge {:a 1} {:b 2} {:c 3}))
  #_(clojure.core/-> {:a 1}
                     (app.lib.util.perf/fast-merge* {:b 2})
                     (app.lib.util.perf/fast-merge* {:c 3}))

  (macroexpand '(fast-merge {:a 1} {:b 2} {:c 3}))
  #_(app.lib.util.perf/fast-merge* (app.lib.util.perf/fast-merge* {:a 1} {:b 2}) {:c 3})

  (macroexpand '(fast-merge {:a 1}))
  #_{:a 1}

  (criterium.core/quick-bench
    (merge {:a 1} {:b 2}))
  #_"Execution time mean : 301,177551 ns"

  (criterium.core/quick-bench
    (fast-merge {:a 1} {:b 2}))
  #_"Execution time mean : 39,030482 ns"

  (criterium.core/quick-bench
    (merge nil {:a 1 :b 2}))
  #_"Execution time mean : 348,619753 ns"

  (criterium.core/quick-bench
    (fast-merge nil {:a 1 :b 2}))
  #_"Execution time mean : 3,359931 ns"

  (criterium.core/quick-bench
    (merge {} {:a 1 :b 2}))
  #_"Execution time mean : 326,284224 ns"

  (criterium.core/quick-bench
    (fast-merge {} {:a 1 :b 2}))
  #_"Execution time mean : 68,303777 ns"

  (criterium.core/quick-bench
    (merge {:a 1 :b 2} nil))
  #_"Execution time mean : 126,896436 ns"

  (criterium.core/quick-bench
    (fast-merge {:a 1 :b 2} nil))
  #_"Execution time mean : 3,856951 ns"

  (criterium.core/quick-bench
    (merge {:a 1 :b 2} {}))
  #_"Execution time mean : 255,907409 ns"

  (criterium.core/quick-bench
    (fast-merge {:a 1 :b 2} {}))
  #_"Execution time mean : 5,309984 ns"

  (criterium.core/quick-bench
    (empty? {}))
  #_"Execution time mean : 10,263854 ns"

  (criterium.core/quick-bench
    (counted? {}))
  #_"Execution time mean : 3,467598 ns"

  (criterium.core/quick-bench
    (empty-coll? {}))
  #_"Execution time mean : 4,772792 ns"

  (criterium.core/quick-bench
    (empty-coll? nil))
  #_"Execution time mean : 3,263740 ns"

  (criterium.core/quick-bench
    (not (empty-coll? {})))
  #_"Execution time mean : 5,013912 ns"

  (criterium.core/quick-bench
    (not-empty-coll? {}))
  #_"Execution time mean : 4,467668 ns"

  (criterium.core/quick-bench
    (dissoc {} :a :b :c :d :e))
  #_"Execution time mean : 160,335317 ns"

  (criterium.core/quick-bench
    (inline-dissoc {} :a :b :c :d :e))
  #_"Execution time mean : 22,686203 ns"

  (criterium.core/quick-bench
    (select-keys {:a 1, :b 2, :c nil, :d 4, :e 5}, [:a :b :c :x :y])
    #_{:a 1, :b 2, :c nil})
  #_"Execution time mean : 825,281317 ns"

  (criterium.core/quick-bench
    (fast-select-keys {:a 1, :b 2, :c nil, :d 4, :e 5}, [:a :b :c :x :y])
    #_{:a 1, :b 2})
  #_"Execution time mean : 98,488658 ns"


  :comment)


(defmacro inline-str
  "Build string inline using StringBuilder."
  [& args]
  (let [append (keep #(cond
                        (or (symbol? %) (list? %)), (list '.append (list 'clojure.core/str %))
                        (nil? %) nil
                        :else (list '.append %))
                     args)]
    `(-> (StringBuilder.)
         ~@append
         (.toString))))

(comment
  (macroexpand-1 '(inline-str "String" 0 (inc 1) x nil {}))
  (let [x :x]
    (inline-str "String" 0 (inc 1) x nil {}))

  (criterium.core/quick-bench
    (str 1 2 3))
  #_"Execution time mean : 143,570889 ns"

  (criterium.core/quick-bench
    (inline-str 1 2 3))
  #_"Execution time mean : 21,467049 ns"

  (criterium.core/quick-bench
    (str "1" "2" "3"))
  #_"Execution time mean : 83,158596 ns"

  (criterium.core/quick-bench
    (inline-str "1" "2" "3"))
  #_"Execution time mean : 5,262824 ns"

  (criterium.core/quick-bench
    (str (inc 1) (inc 2) (inc 3)))
  #_"Execution time mean : 148,604234 ns"

  (criterium.core/quick-bench
    (inline-str (inc 1) (inc 2) (inc 3)))
  #_"Execution time mean : 83,900023 ns"

  'comment)

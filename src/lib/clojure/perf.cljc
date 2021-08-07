(ns lib.clojure.perf
  "Replacement of core functions for better performance."
  #?(:clj (:import (clojure.lang Associative))))

#?(:clj (set! *warn-on-reflection* true) :cljs (set! *warn-on-infer* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn fast-assoc*
  "Assoc using direct call to `Associative.assoc`."
  [m k v]
  (let [m (or m {})]
    #?(:clj  (.assoc ^Associative m k v)
       :cljs (-assoc ^IAssociative m k v))))

(defmacro fast-assoc
  "Replacement for `clojure.core/assoc`.
   Multiple keys are assoc'ed as multiple calls to `fast-assoc*`."
  [m & kvs]
  (assert (even? (count kvs)))
  `(-> ~m
       ~@(map (fn [[k v]] (list 'lib.clojure.perf/fast-assoc* k v))
              (partition 2 kvs))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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
       ~@(map #(list 'lib.clojure.perf/fast-merge* %) maps)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro inline-dissoc
  "Call `clojure.core/dissoc` for multiple keys without loop over collection."
  [m & ks]
  `(-> ~m
       ~@(map (fn [k] (list 'clojure.core/dissoc k)) ks)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(comment
  (assoc {:a 1} :b 2)                             ; "Execution time mean :  39,736546 ns"
  (fast-assoc {:a 1} :b 2)                        ; "Execution time mean :  25,699835 ns, 35% faster"
  (pos? (count nil))                              ; "Execution time mean :   2,960623 ns"
  (zero? (count nil))                             ; "Execution time mean :   2,952321 ns"
  (zero? (count {}))                              ; "Execution time mean :   3,301519 ns"
  (not-empty {})                                  ; "Execution time mean :  10,164182 ns"
  (some? nil)                                     ; "Execution time mean :   3,269397 ns"
  (nil? nil)                                      ; "Execution time mean :   2,923687 ns"
  (not nil)                                       ; "Execution time mean :   3,396421 ns"

  (macroexpand-1 '(fast-merge {:a 1} {:b 2} {:c 3}))
  #_(clojure.core/-> {:a 1}
                     (lib.clojure.perf/fast-merge* {:b 2})
                     (lib.clojure.perf/fast-merge* {:c 3}))

  (macroexpand '(fast-merge {:a 1} {:b 2} {:c 3}))
  #_(lib.clojure.perf/fast-merge* (lib.clojure.perf/fast-merge* {:a 1} {:b 2}) {:c 3})

  (macroexpand '(fast-merge {:a 1}))
  #_{:a 1}

  (merge {:a 1} {:b 2})                           ; "Execution time mean : 301,177551 ns"
  (fast-merge {:a 1} {:b 2})                      ; "Execution time mean :  39,030482 ns"
  (merge nil {:a 1 :b 2})                         ; "Execution time mean : 348,619753 ns"
  (fast-merge nil {:a 1 :b 2})                    ; "Execution time mean :   3,359931 ns"
  (merge {} {:a 1 :b 2})                          ; "Execution time mean : 326,284224 ns"
  (fast-merge {} {:a 1 :b 2})                     ; "Execution time mean :  68,303777 ns"
  (merge {:a 1 :b 2} nil)                         ; "Execution time mean : 126,896436 ns"
  (fast-merge {:a 1 :b 2} nil)                    ; "Execution time mean :   3,856951 ns"
  (merge {:a 1 :b 2} {})                          ; "Execution time mean : 255,907409 ns"
  (fast-merge {:a 1 :b 2} {})                     ; "Execution time mean :   5,309984 ns"
  (empty? {})                                     ; "Execution time mean :  10,263854 ns"
  (counted? {})                                   ; "Execution time mean :   3,467598 ns"
  (empty-coll? {})                                ; "Execution time mean :   4,772792 ns"
  (empty-coll? nil)                               ; "Execution time mean :   3,263740 ns"
  (not (empty-coll? {}))                          ; "Execution time mean :   5,013912 ns"
  (not-empty-coll? {})                            ; "Execution time mean :   4,467668 ns"
  (dissoc {} :a :b :c :d :e)                      ; "Execution time mean : 160,335317 ns"
  (inline-dissoc {} :a :b :c :d :e)               ; "Execution time mean :  22,686203 ns"

  (select-keys {:a 1, :b 2, :c nil, :d 4, :e 5}   ; "Execution time mean : 825,281317 ns"
               [:a :b :c :x :y])
  #_{:a 1, :b 2, :c nil}

  (fast-select-keys {:a 1, :b 2, :c nil, :d 4, :e 5} ; "Execution time mean : 98,488658 ns"
                    [:a :b :c :x :y])
  #_{:a 1, :b 2}

  :comment)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

  (str 1 2 3)                                     ; Execution time mean : 143,570889 ns"
  (inline-str 1 2 3)                              ; Execution time mean :  21,467049 ns"
  (str "1" "2" "3")                               ; Execution time mean :  83,158596 ns"
  (inline-str "1" "2" "3")                        ; Execution time mean :   5,262824 ns"
  (str (inc 1) (inc 2) (inc 3))                   ; Execution time mean : 148,604234 ns"
  (inline-str (inc 1) (inc 2) (inc 3))            ; Execution time mean :  83,900023 ns"

  'comment)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

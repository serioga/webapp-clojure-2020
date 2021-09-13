(ns lib.clojure.perf
  "Faster implementation of some core functions."
  (:import (clojure.lang Associative Counted ITransientAssociative ITransientMap IPersistentMap)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn fast-merge
  "Merges two hash-maps `a` and `b` using direct assoc and skipping empty input.
   (!) Does not preserve meta of the empty `a`."
  [a b]
  (cond (or (nil? b) (zero? (.count ^Counted b))) a
        (or (nil? a) (zero? (.count ^Counted a))) b
        :else (reduce-kv (fn [m k v] (.assoc ^Associative m k v))
                         a b)))

(defn fast-merge!
  "Merges hash-map `b` into transient map `a` using direct assoc."
  [a b]
  (if b (reduce-kv (fn [m k v] (.assoc ^ITransientAssociative m k v))
                   a b)
        a))

(comment
  (merge {0 0} {1 1})                                       ;Execution time mean : 310,933558 ns
  (into {0 0} {1 1})                                        ;Execution time mean : 417,663587 ns
  (fast-merge {0 0} {1 1})                                  ;Execution time mean :  65,105927 ns
  (-> (fast-merge! (transient {0 0}) {1 1})                 ;Execution time mean : 123,863403 ns
      (persistent!))

  (merge {0 0} {1 1 2 2 3 3 4 4 5 5})                       ;Execution time mean : 673,807932 ns
  (into {0 0} {1 1 2 2 3 3 4 4 5 5})                        ;Execution time mean : 723,114249 ns
  (fast-merge {0 0} {1 1 2 2 3 3 4 4 5 5})                  ;Execution time mean : 399,628167 ns
  (-> (fast-merge! (transient {1 1}) {1 1 2 2 3 3 4 4 5 5}) ;Execution time mean : 263,551012 ns
      (persistent!))

  (merge {0 0 1 1 2 2 3 3 4 4} {5 5})                       ;Execution time mean : 370,321250 ns
  (into {0 0 1 1 2 2 3 3 4 4} {5 5})                        ;Execution time mean : 488,377325 ns
  (fast-merge {0 0 1 1 2 2 3 3 4 4} {5 5})                  ;Execution time mean : 119,066136 ns
  (-> (fast-merge! (transient {0 0 1 1 2 2 3 3 4 4}) {5 5}) ;Execution time mean : 152,394523 ns
      (persistent!))

  (merge {0 0 1 1 2 2 3 3 4 4} {5 5 6 6 7 7 8 8 9 9})       ;Execution time mean : 1788,143 ns
  (into {0 0 1 1 2 2 3 3 4 4} {5 5 6 6 7 7 8 8 9 9})        ;Execution time mean : 1661,969 ns
  (fast-merge {0 0 1 1 2 2 3 3 4 4} {5 5 6 6 7 7 8 8 9 9})  ;Execution time mean : 1568,274 ns
  (-> (fast-merge! (transient {0 0 1 1 2 2 3 3 4 4})        ;Execution time mean : 1324,278 ns
                   {5 5 6 6 7 7 8 8 9 9})
      (persistent!))

  (merge nil {0 0})                                         ;Execution time mean : 301,955790 ns
  (fast-merge nil {0 0})                                    ;Execution time mean :   5,475567 ns

  (merge {} {0 0})                                          ;Execution time mean : 274,920615 ns
  (fast-merge {} {0 0})                                     ;Execution time mean :   6,310723 ns

  (merge {0 0} nil)                                         ;Execution time mean : 126,049276 ns
  (fast-merge {0 0} nil)                                    ;Execution time mean :   7,454297 ns
  (-> (fast-merge! (transient {0 0}) nil)                   ;Execution time mean :  87,183660 ns
      (persistent!))

  (merge {0 0} {})                                          ;Execution time mean : 267,252445 ns
  (fast-merge {0 0} {})                                     ;Execution time mean :   7,414466 ns
  (-> (fast-merge! (transient {0 0}) {})                    ;Execution time mean : 98,556042 ns
      (persistent!))

  (merge nil nil)                                           ;Execution time mean :  68,336964 ns
  (fast-merge nil nil)                                      ;Execution time mean :   0,298792 ns

  (merge {} {})                                             ;Execution time mean : 246,174180 ns
  (fast-merge {} {})                                        ;Execution time mean :   7,683187 ns
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro inline-assoc
  "Inlined `clojure.core/assoc.`"
  [m & kvs]
  (assert (even? (count kvs)))
  `(let [^Associative m# (or ~m {})]
     (-> m# ~@(map (fn [[k v]] (list '.assoc k v))
                   (partition 2 kvs)))))

(defmacro inline-assoc!
  "Inlined `clojure.core/assoc!.`"
  [m & kvs]
  (assert (even? (count kvs)))
  `(let [^ITransientAssociative m# ~m]
     (-> m# ~@(map (fn [[k v]] (list '.assoc k v))
                   (partition 2 kvs)))))

(comment
  (def a ^:test {0 0 1 0 2 0 3 0 4 0 5 0 6 0 7 0 8 0 9 0})
  (macroexpand-1 '(inline-assoc a 10 1 11 1 12 1 13 1 14 1 15 1 16 1 17 1 18 1 19 1))

  (assoc a 10 1 11 1 12 1 13 1 14 1                         ;Execution time mean : 2,315429 µs
           15 1 16 1 17 1 18 1 19 1)
  (inline-assoc a 10 1 11 1 12 1 13 1 14 1                  ;Execution time mean : 1,136405 µs
                  15 1 16 1 17 1 18 1 19 1)
  (-> (inline-assoc! (transient a)                          ;Execution time mean : 1,123020 µs
                     10 1 11 1 12 1 13 1 14 1
                     15 1 16 1 17 1 18 1 19 1)
      (persistent!))
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro inline-dissoc
  "Inlined `clojure.core/dissoc`."
  [m & ks]
  `(let [^IPersistentMap m# ~m]
     (-> m# ~@(map #(list '.without %) ks))))

(defmacro inline-dissoc!
  "Inlined `clojure.core/dissoc!`."
  [m & ks]
  `(let [^ITransientMap m# ~m]
     (-> m# ~@(map #(list '.without %) ks))))

(comment
  (macroexpand-1 '(inline-dissoc {} 1 2 3 4 5))
  (dissoc {} 1 2 3 4 5)                                     ;Execution time mean :  94,260732 ns
  (inline-dissoc {} 1 2 3 4 5)                              ;Execution time mean :  19,025974 ns
  (-> (inline-dissoc! (transient {}) 1 2 3 4 5)             ;Execution time mean :  44,977257 ns
      (persistent!))

  (dissoc {1 1 2 2 3 3 4 4 5 5} 1 2 3 4 5)                  ;Execution time mean : 300,851040 ns
  (inline-dissoc {1 1 2 2 3 3 4 4 5 5} 1 2 3 4 5)           ;Execution time mean : 203,756272 ns
  (-> (inline-dissoc! (transient {1 1 2 2 3 3 4 4 5 5})     ;Execution time mean : 158,575963 ns
                      1 2 3 4 5)
      (persistent!))
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro inline-str
  "Builds string inline."
  [& args]
  `(-> (StringBuilder.)
       ~@(->> args (map #(list '.append (cond->> % (list? %) (list 'clojure.core/str)))))
       (.toString)))

(comment
  (macroexpand-1 '(inline-str 1 2 3 "" (do nil)))
  (str 1 2 3 "" :x (inc 3) [] {})
  (inline-str 1 2 3 "" :x (inc 3) [] {})

  (str "a: " 1 " b: " 2 " c: " 3)                           ;Execution time mean : 317,642476 ns
  #_"a: 1 b: 2 c: 3"
  (inline-str "a: " 1 " b: " 2 " c: " 3)                    ;Execution time mean :  45,411832 ns
  #_"a: 1 b: 2 c: 3"

  (str :a " " 1 " " :b " " 2 " " :c " " 3)                  ;Execution time mean : 527,635347 ns
  #_":a 1 :b 2 :c 3"
  (inline-str :a " " 1 " " :b " " 2 " " :c " " 3)           ;Execution time mean :  75,721557 ns
  #_":a 1 :b 2 :c 3"

  (str :a \space 1 \space :b \space 2 \space :c \space 3)   ;Execution time mean : 556,198887 ns
  #_":a 1 :b 2 :c 3"
  (inline-str :a \space 1 \space :b \space 2 \space         ;Execution time mean :  84,851354 ns
              :c \space 3)
  #_":a 1 :b 2 :c 3"

  (str (comment nil) (comment nil) (comment nil)            ;Execution time mean : 165,238901 ns
       (comment nil) (comment nil))
  #_""
  (inline-str (comment nil) (comment nil) (comment nil)     ;Execution time mean :  21,796580 ns
              (comment nil) (comment nil))
  #_""
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

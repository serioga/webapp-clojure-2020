(ns lib.clojure.perf
  "Faster implementation of some core functions."
  (:import (clojure.lang Counted)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn merge-not-empty
  "Merges two hash-maps `a` and `b` skipping empty input.
   (!) Does not preserve meta of the empty `a`."
  [a b]
  (if b
    (if a
      (if (zero? (.count ^Counted b))
        a
        (if (zero? (.count ^Counted a))
          b
          (merge a b)))
      b)
    a))

(comment
  (merge {0 0} {5 5 6 6 7 7 8 8 9 9})                       ;Execution time mean : 701,882584 ns
  (merge-not-empty {0 0} {5 5 6 6 7 7 8 8 9 9})             ;Execution time mean : 706,529319 ns

  (merge nil {0 0})                                         ;Execution time mean : 301,955790 ns
  (merge-not-empty nil {0 0})                               ;Execution time mean :   5,475567 ns

  (merge {} {0 0})                                          ;Execution time mean : 274,920615 ns
  (merge-not-empty {} {0 0})                                ;Execution time mean :   6,678426 ns

  (merge {0 0} nil)                                         ;Execution time mean : 126,049276 ns
  (merge-not-empty {0 0} nil)                               ;Execution time mean :   6,130941 ns

  (merge {0 0} {})                                          ;Execution time mean : 267,252445 ns
  (merge-not-empty {0 0} {})                                ;Execution time mean :   7,414466 ns

  (merge nil nil)                                           ;Execution time mean :  68,336964 ns
  (merge-not-empty nil nil)                                 ;Execution time mean :   0,298792 ns

  (merge {} {})                                             ;Execution time mean : 246,174180 ns
  (merge-not-empty {} {})                                   ;Execution time mean :   6,895146 ns
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro inline-str
  "Builds string inline."
  [& args]
  `(-> (StringBuilder.)
       ~@(->> args (map #(list '.append (cond->> % ((some-fn symbol? list?) %) (list 'clojure.core/str)))))
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

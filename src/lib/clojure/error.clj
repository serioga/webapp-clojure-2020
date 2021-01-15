(ns lib.clojure.error
  (:refer-clojure :exclude [assert]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn print-str*
  "Similar to `print-str` but ignoring nils and empty strings."
  [& tokens]
  (if-some [tokens (seq tokens)]
    (-> ^StringBuilder (reduce (fn [^StringBuilder sb, ^Object x]
                                 (if-some [s (some-> x
                                                     (.toString)
                                                     (as-> <> (when-not (.isEmpty <>) <>)))]
                                   (-> sb (.append " "), (.append s))
                                   sb))
                               (StringBuilder.) tokens)
        (.substring 1)
        (str))
    ""))

(comment
  (require '[criterium.core])
  (criterium.core/quick-bench
    (print-str "a" nil "b" "" "c")
    #_"a nil b  c")
  #_"Execution time mean : 2,571438 µs"
  (criterium.core/quick-bench
    (print-str* "a" nil "b" "" "c")
    #_"a b c")
  #_"Execution time mean : 105,686375 ns")

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro expand-msg*
  "Convert vector of message tokens to string.
   All symbolic tokens are wrapped with `pr-str`."
  [tokens]
  (cond
    (vector? tokens)
    `(print-str* ~@(let [wrap? (complement string?)
                         wrap-pr (fn [v] (if (wrap? v) (list pr-str v), v))]
                     (map wrap-pr tokens)))
    :else
    `(str ~tokens)))

(comment
  (expand-msg* ["a" "b" "c" 'd (str "e") {:f "f"}])
  #_"a b c d \"e\" {:f \"f\"}"
  (expand-msg* (str 1 2 3))
  #_"123"
  (expand-msg* :a)
  #_":a"
  (let [x {:a "1"}
        y "y"
        z :z]
    (expand-msg* [1 2 3 x y z {:a "1"}]))
  #_"1 2 3 {:a \"1\"} \"y\" :z {:a \"1\"}"
  (clojure.core/ex-info (expand-msg* [1 2 3 4 5 {1 2}]) {}))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert
  "Simple assert for predicate function `pred`.
   Returns `x` if `(pred x)` is logical true, otherwise throws AssertionError.
   Expands vector in `msg` to message string."
  ([x pred]
   `(let [x# ~x]
      (when-not (try (~pred x#) (catch Throwable _# false))
        (throw (new AssertionError (print-str* "Assert failed:" '(~pred ~x)
                                               "- input" {:value x# :type (type x#)}))))
      x#))
  ([x pred msg]
   `(let [x# ~x]
      (when-not (try (~pred x#) (catch Throwable _# false))
        (throw (new AssertionError (print-str* (expand-msg* ~msg)
                                               "- Assert failed:" '(~pred ~x)
                                               "- input" {:value x# :type (type x#)}))))

      x#)))

(defmacro assert?
  "Same as `assert` but returns `true` when assert passed.
   To be used in :pre/:post conditions."
  ([x pred]
   `(do (assert ~x ~pred) true))
  ([x pred msg]
   `(do (assert ~x ~pred ~msg) true)))

(comment

  (assert "1" string?) #_"1"
  (assert? "1" string?) #_true
  (assert (inc 0) string?) #_"Assert failed: (string? (inc 0)) - input {:value 1, :type java.lang.Long}"
  (assert? (inc 0) string?) #_"Assert failed: (string? (inc 0)) - input {:value 1, :type java.lang.Long}"
  (assert nil string?) #_"Assert failed: (string? nil) - input {:value nil, :type nil}"
  (assert (inc 0) string? "Require string") #_"Require string - Assert failed: (string? (inc 0)) - input {:value 1, :type java.lang.Long}"
  (assert 1 first) #_"Assert failed: (first 1) - input {:value 1, :type java.lang.Long}")

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

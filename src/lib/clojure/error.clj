(ns lib.clojure.error
  (:refer-clojure :exclude [assert]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert
  "Simple assert for predicate function `pred`.
   Returns `x` if `(pred x)` is logical true, otherwise throws AssertionError.
   Expands vector in `msg` to message string."
  ([x pred]
   `(let [x# ~x]
      (when-not (try (~pred x#) (catch Throwable _# false))
        (throw (new AssertionError (str "Assert failed: " '(~pred ~x)
                                        " - input " {:value x# :type (type x#)}))))
      x#))
  ([x pred msg]
   `(let [x# ~x]
      (when-not (try (~pred x#) (catch Throwable _# false))
        (throw (new AssertionError (str ~msg " - Assert failed: " '(~pred ~x)
                                        " - input " {:value x# :type (type x#)}))))

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

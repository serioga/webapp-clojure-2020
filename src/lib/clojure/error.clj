(ns lib.clojure.error
  (:refer-clojure :exclude [assert]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn assert-pred-error*
  "Returns predicate error for `assert` macro."
  [x pred]
  (try
    (when-not (pred x)
      (if-some [explain (-> pred meta :assert/explain)]
        {:explain (explain x)}
        {}))
    (catch Throwable e
      {:ex-message (ex-message e)})))

(defn assert-ex-message*
  "Returns exception message for `assert` macro."
  ([x msg]
   (str msg " - Assert failed on input " {:value x}))
  ([x msg pred-form pred-error]
   (let [pred-error (not-empty pred-error)]
     (str msg " - Assert failed: " pred-form " - input " {:value x :type (type x)}
          (when pred-error " - ") pred-error))))

(defn assert-ex-data*
  "Returns ex-data for `assert` macro."
  []
  {::failure :assert-failed})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert
  "Simple assert for predicate function `pred`.
   Returns `x` if `(pred x)` is logical true, otherwise throws AssertionError.
   Expands vector in `msg` to message string.
   Uses :assert/explain from `pred`s meta for extra failure explanation (i.e. for spec asserts)."
  ([x]
   `(let [x# ~x]
      (when-not x#
        (throw (ex-info (assert-ex-message* x# (pr-str '~x)) (assert-ex-data*))))
      x#))
  ([x pred]
   `(let [x# ~x, pred-error# (assert-pred-error* x# ~pred)]
      (when pred-error#
        (throw (ex-info (assert-ex-message* x# (pr-str '~x) '~pred pred-error#) (assert-ex-data*))))
      x#))
  ([x pred msg]
   `(let [x# ~x, pred-error# (assert-pred-error* x# ~pred)]
      (when pred-error#
        (throw (ex-info (assert-ex-message* x# (str ~msg) '~pred pred-error#) (assert-ex-data*))))
      x#)))

(defmacro assert?
  "Same as `assert` but returns `true` when assert passed.
   To be used in :pre/:post conditions."
  ([x pred]
   `(do (assert ~x ~pred) true))
  ([x pred msg]
   `(do (assert ~x ~pred ~msg) true)))

(comment
  (macroexpand '(assert "1"))
  (macroexpand '(assert "1" string?))
  (macroexpand '(assert "1" string? "Message"))

  (assert "1" string?)
  #_"1"
  (assert? "1" string?)
  #_true
  (assert (inc 0) string?)
  ;;(inc 0) - Assert failed: string? - input {:value 1, :type java.lang.Long}
  (assert? (inc 0) string?)
  ;;(inc 0) - Assert failed: string? - input {:value 1, :type java.lang.Long}
  (assert nil)
  ;;nil - Assert failed on input {:value nil}
  (assert nil string?)
  ;;nil - Assert failed: string? - input {:value nil, :type nil}
  (assert "" number?)
  ;;"" - Assert failed: number? - input {:value "", :type java.lang.String}
  (assert (inc 0) string? "Require string")
  ;;Require string - Assert failed: string? - input {:value 1, :type java.lang.Long}
  (assert (inc 0) string? ["Require string" 0])
  ;;["Require string" 0] - Assert failed: string? - input {:value 1, :type java.lang.Long}
  (assert 1 first)
  ;;1 - Assert failed: first - input {:value 1, :type java.lang.Long} - {:ex-message "Don't know how to create ISeq from: java.lang.Long"}
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

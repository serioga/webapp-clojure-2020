(ns lib.clojure.error
  (:refer-clojure :exclude [assert]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn assert-ex-message*
  "Returns exception message for `assert` macros."
  ([x msg]
   (str msg " - Assert failed on input " {:value x :type (type x)}))
  ([x msg form]
   (str msg " - Assert failed: " form " - input " {:value x :type (type x)})))

(defn assert-ex-data*
  "Returns ex-data for `assert` macro."
  []
  {::failure :assert-failed})

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert
  "Evaluates expr and throws an exception if it does not evaluate to logical true.
   Same as clojure.core/assert but with improved exception message and data."
  ([x]
   `(assert ~x nil))
  ([x msg]
   (let [x-sym (gensym)]
     `(let [~x-sym ~x]
        (when-not ~x-sym
          (throw (ex-info ~(if msg `(assert-ex-message* ~x-sym (str ~msg) (pr-str '~x))
                                   `(assert-ex-message* ~x-sym (pr-str '~x)))
                          (assert-ex-data*))))))))

(comment
  (macroexpand '(assert-expr "1"))
  (macroexpand '(assert-expr "1" "Message"))

  (assert (= 1 (inc 0)))
  #_nil

  (assert (= 1 2))
  ;;clojure.lang.ExceptionInfo: (= 1 2) - Assert failed on input {:value false}

  (assert nil)
  ;;clojure.lang.ExceptionInfo: nil - Assert failed on input

  (assert (= 1 2) "Require 1=2")
  ;;clojure.lang.ExceptionInfo: Require 1=2 - Assert failed: (= 1 2) - input {:value false, :type java.lang.Boolean}
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert-pred
  "Evaluates `(pred x)` and throws an exception if it does not evaluate to logical true."
  ([x pred]
   `(assert-pred ~x ~pred nil))
  ([x pred msg]
   `(let [x# ~x, failure# (try (not (~pred x#)) (catch Throwable e# e#))]
      (when failure#
        (throw (ex-info (assert-ex-message* x# ~(if msg `(str ~msg) `(pr-str '~x)) '~pred)
                        (assert-ex-data*)
                        (when-not (true? failure#) failure#)))))))

(comment
  (macroexpand '(assert-pred "1" string?))
  (macroexpand '(assert-pred "1" string? "Message"))

  (assert-pred "1" string?)
  #_nil

  (assert-pred (inc 0) string?)
  ;;clojure.lang.ExceptionInfo: (inc 0) - Assert failed: string? - input {:value 1, :type java.lang.Long}

  (assert-pred nil string?)
  ;;clojure.lang.ExceptionInfo: nil - Assert failed: string? - input {:value nil, :type nil}

  (assert-pred "" number?)
  ;;clojure.lang.ExceptionInfo: "" - Assert failed: number? - input {:value "", :type java.lang.String}

  (assert-pred (inc 0) string? "Require string")
  ;;clojure.lang.ExceptionInfo: Require string - Assert failed: string? - input {:value 1, :type java.lang.Long}

  (assert-pred (inc 0) string? ["Require string" 0])
  ;;clojure.lang.ExceptionInfo: ["Require string" 0] - Assert failed: string? - input {:value 1, :type java.lang.Long}

  (assert-pred 1 first)
  ;;clojure.lang.ExceptionInfo: 1 - Assert failed: first - input {:value 1, :type java.lang.Long}
  ;;java.lang.IllegalArgumentException: Don't know how to create ISeq from: java.lang.Long
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

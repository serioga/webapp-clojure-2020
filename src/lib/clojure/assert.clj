(ns lib.clojure.assert
  "Helper macros for runtime assertion with min overhead and max clarity."
  (:refer-clojure :exclude [assert])
  (:require [lib.clojure.lang :as lang]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti ex-data-fn
  "Returns ex-data function by `id`."
  {:arglists '([id & args])}
  lang/first-arg)

(defmethod ex-data-fn ::test
  [_ test]
  (fn [_throwable] {::test test}))

(defmethod ex-data-fn ::value
  [_ value]
  (fn [_throwable]
    (cond-> {::value value}
      (and (some? value)
           (not (identical? value ::invalid))) (assoc ::type (type value)))))

(defn exception
  "Returns assertion exception with message:
  `{message} - Assert failed: {form}` and ex-data from `(edf throwable)`.
  Arguments `message`, `edf` and `throwable` can be `nil`."
  [form message edf throwable]
  (-> (ex-info (str (some-> message (str " - "))
                    "Assert failed: " (cond-> form (nil? form) pr-str))
               (-> (when edf (edf throwable))
                   (assoc ::failure :assertion-failed))
               throwable)
      (#'clojure.core/elide-top-frames "lib.clojure.assert$exception")))

(def ^:const x*
  "The symbol representing evaluated `~x` in the [[assert-impl]] macro."
  'x)

(defmacro assert-impl
  "Evaluates `x` and `test`, catches exception and throws an exception if `test`
  does not evaluate to logical true or other exceptions were thrown. Evaluated
  `x` is available in implementing macros as `x*`, which is `::invalid` if
   evaluation failed. Returns `true` if assertion passed."
  [x test form message edf]
  `(if-let [{e# :throwable, ~x* :x, :or {~x* ::invalid}}
            (try (let [~x* ~x] (try (when-not ~test {:x ~x*})
                                    (catch Throwable e# {:x ~x* :throwable e#})))
                 (catch Throwable e# {:throwable e#}))]
     (throw (exception ~form ~message ~edf e#))
     true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert
  "Evaluates expr and throws an exception if it does not evaluate to logical
  true. Similar to `clojure.core/assert` but with improved exception message and
  ex-data. Returns `true`. Attaches failed `::test` (`false` or `nil`) in
  ex-data."
  ([x] `(assert ~x nil))
  ([x message]
   `(assert-impl ~x ~x* '(~'assert ~x) ~message (ex-data-fn ::test ~x*))))

(comment
  (macroexpand '(assert "1"))
  (macroexpand '(assert "1" "Message"))

  (assert (= 1 (inc 0)))
  #_true

  (assert (= 1 2))
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert (= 1 2))
  ;; #:lib.clojure.assert{:test false, :failure :assertion-failed}

  (assert (or false nil))
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert (or false nil))
  ;; #:lib.clojure.assert{:test nil, :failure :assertion-failed}

  (assert (or nil false))
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert (or nil false))
  ;; #:lib.clojure.assert{:test false, :failure :assertion-failed}

  (assert (= 1 2) "Require 1=2")
  ;;clojure.lang.ExceptionInfo: Require 1=2 - Assert failed: (assert (= 1 2))
  ;; #:lib.clojure.assert{:test false, :failure :assertion-failed}

  (assert (/ 1 0))
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert (/ 1 0))
  ;; #:lib.clojure.assert{:test :lib.clojure.assert/invalid, :failure :assertion-failed}
  ;;java.lang.ArithmeticException: Divide by zero

  (try (assert (/ 1 0))
       (catch Throwable e
         (lib.clojure-tools-logging.logger/log-throwable e nil)))
  ;;Assert failed: (assert (/ 1 0))  >  Divide by zero   $   #:lib.clojure.assert{:test :lib.clojure.assert/invalid, :failure :assertion-failed}
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert-pred
  "Evaluates `(pred x)` and throws an exception if it does not evaluate to
  logical true. Returns `true`. Attaches failed `::value` in ex-data."
  ([x pred] `(assert-pred ~x ~pred nil))
  ([x pred message]
   `(assert-impl ~x (~pred ~x*) '(~'assert-pred ~x ~pred) ~message (ex-data-fn ::value ~x*))))

(comment
  (macroexpand '(assert-pred "1" string?))
  (macroexpand '(assert-pred "1" string? "Message"))

  (assert-pred "1" string?)
  #_true

  (assert-pred (inc 0) string?)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-pred (inc 0) string?)
  ;; #:lib.clojure.assert{:value 1, :type java.lang.Long, :failure :assertion-failed}

  (assert-pred nil string?)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-pred nil string?)
  ;; #:lib.clojure.assert{:value nil, :failure :assertion-failed}

  (assert-pred "" number?)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-pred "" number?)
  ;; #:lib.clojure.assert{:value "", :type java.lang.String, :failure :assertion-failed}

  (assert-pred (inc 0) string? "Require string")
  ;;clojure.lang.ExceptionInfo: Require string - Assert failed: (assert-pred (inc 0) string?)
  ;; #:lib.clojure.assert{:value 1, :type java.lang.Long, :failure :assertion-failed}

  (assert-pred (inc 0) string? ["Require string"])
  ;;clojure.lang.ExceptionInfo: ["Require string"] - Assert failed: (assert-pred (inc 0) string?)
  ;; #:lib.clojure.assert{:value 1, :type java.lang.Long, :failure :assertion-failed}

  (assert-pred 1 first)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-pred 1 first)
  ;; #:lib.clojure.assert{:value 1, :type java.lang.Long, :failure :assertion-failed}
  ;;java.lang.IllegalArgumentException: Don't know how to create ISeq from: java.lang.Long

  (try (assert-pred 1 first)
       (catch Throwable e
         (lib.clojure-tools-logging.logger/log-throwable e nil)))
  ;;Assert failed: (assert-pred 1 first)  >  Don't know how to create ISeq from: java.lang.Long   $   #:lib.clojure.assert{:value 1, :type java.lang.Long, :failure :assertion-failed}

  (assert-pred (/ 1 0) double?)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-pred (/ 1 0) double?)
  ;; #:lib.clojure.assert{:value :lib.clojure.assert/invalid, :failure :assertion-failed}
  ;;java.lang.ArithmeticException: Divide by zero
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro assert-try
  "Evaluates `(f x)` and fails assertion if `(f x)` throws exception. Returns
  `true`. Attaches failed `::value` in ex-data."
  ([x f] `(assert-try ~x ~f nil))
  ([x f message]
   `(assert-impl ~x (do (~f ~x*) true) '(~'assert-try ~x ~f) ~message (ex-data-fn ::value ~x*))))

(comment
  (macroexpand '(assert-try 10 test-fn))

  (assert-try 10 identity)
  #_true

  (defn test-fn [x] (throw (ex-info "Oops" {:test/x x})))

  (assert-try 10 test-fn)
  ;;clojure.lang.ExceptionInfo: Assert failed: (assert-try 10 test-fn)
  ;; #:lib.clojure.assert{:value 10, :type java.lang.Long, :failure :assertion-failed}
  ;;clojure.lang.ExceptionInfo: Oops
  ;; #:test{:x 10}

  (assert-try 10 test-fn "Message")
  ;;clojure.lang.ExceptionInfo: Message - Assert failed: (assert-try 10 test-fn)
  ;; #:lib.clojure.assert{:value 10, :type java.lang.Long, :failure :assertion-failed}
  ;;clojure.lang.ExceptionInfo: Oops
  ;; #:test{:x 10}

  (try (assert-try 10 test-fn "Message")
       (catch Throwable e
         (lib.clojure-tools-logging.logger/log-throwable e nil)
         (throw e)))
  ;;Message - Assert failed: (assert-try 10 test-fn)  >  Oops   $   #:lib.clojure.assert{:value 10, :type java.lang.Long, :failure :assertion-failed}   $   #:test{:x 10}

  (try (assert-try (/ 1 0) test-fn "Message")
       (catch Throwable e
         (lib.clojure-tools-logging.logger/log-throwable e nil)
         (throw e)))
  ;;Message - Assert failed: (assert-try (/ 1 0) test-fn)  >  Divide by zero   $   #:lib.clojure.assert{:value :lib.clojure.assert/invalid, :failure :assertion-failed}
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

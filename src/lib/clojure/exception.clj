(ns lib.clojure.exception
  (:refer-clojure :exclude [ex-info])
  (:require [clojure.tools.logging :as log]
            [lib.clojure.error :as err]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn throwable?
  "Test if `x` is `Throwable`."
  [x]
  (instance? Throwable x))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:const _->_
  "Error message separator for nested exceptions etc."
  "->")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-message-all
  "Collect single message from all nested exceptions."
  [^Throwable ex]
  (loop [msg (ex-message ex)
         cause (ex-cause ex)]
    (if cause
      (recur (err/print-str* msg _->_ (ex-message cause)) (ex-cause cause))
      msg)))

(comment
  (ex-message-all (clojure.core/ex-info "One" {:x :one}
                                        (clojure.core/ex-info "Two" {:x :two}
                                                              (clojure.core/ex-info "Three" {:x :three}))))
  #_"One -> Two -> Three")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-root-cause
  "Find root cause for exception."
  [^Throwable ex]
  (if-let [cause (ex-cause ex)]
    (recur cause)
    ex))

(comment
  (ex-root-cause (clojure.core/ex-info "One" {:x :one}
                                       (clojure.core/ex-info "Two" {:x :two}
                                                             (clojure.core/ex-info "Three" {:x :three})))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-data->log-str
  "Convert ex-data to string for logging."
  [ex-data]
  (when (and (map? ex-data)
             (pos? (count ex-data)))
    (err/print-str* "~//~" (pr-str ex-data))))

(defmacro log-error
  "Log error message."
  {:arglists '([msg*]
               [exception, msg*])}
  [ex & msg-tokens]
  (let [msg (vec msg-tokens)]
    `(let [ex# ~ex
           msg# (err/expand-msg* ~msg)
           msg# (if (throwable? ex#)
                  (err/print-str* (when (not= "" msg#) (err/print-str* msg# _->_))
                                  (ex-message-all ex#)
                                  (some-> ex# ex-data ex-data->log-str))
                  msg#)]
       (log/error ex# msg#))))

(comment
  (log-error (clojure.core/ex-info "One" {:x :one :y "y"}
                                   (clojure.core/ex-info "Two" {:x :two}
                                                         (clojure.core/ex-info "Three" {:x :three})))
             "A" "B" "C" (str "D" "E" "F"))
  #_"A B C \"DEF\" -> One -> Two -> Three ~//~ {:x :one, :y \"y\"}"
  (log-error (clojure.core/ex-info "One" {:x :one}
                                   (clojure.core/ex-info "Two" {:x :two}
                                                         (clojure.core/ex-info "Three" {:x :three})))
             ["A" "B" "C" (str "D" "E" "F")])
  #_"[\"A\" \"B\" \"C\" \"DEF\"] -> One -> Two -> Three ~//~ {:x :one}"
  (log-error (clojure.core/ex-info "One" {:x :one}
                                   (clojure.core/ex-info "Two" {:x :two}
                                                         (clojure.core/ex-info "Three" {:x :three}))))
  #_"One -> Two -> Three ~//~ {:x :one}")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro ex-info
  "Same as `ex-info`, plus:
   - expands vector in `msg` to message string;
   - merges ex-data of `cause` to `map`."
  ([msg] `(ex-info ~msg {}))
  ([msg map]
   `(clojure.core/ex-info (err/expand-msg* ~msg) ~map))
  ([msg map cause]
   `(let [cause# ~cause]
      (clojure.core/ex-info (err/expand-msg* ~msg)
                            (merge (ex-data cause#) ~map)
                            cause#))))

(comment
  (ex-info (vec ["a" "b" "c" 'd (str "e") {:f "f"}]))
  #_"[\"a\" \"b\" \"c\" d \"e\" {:f \"f\"}]"
  (ex-info ["a" "b" "c" 'd (str "e") {:f "f"}])
  #_"a b c d \"e\" {:f \"f\"}"
  (ex-info (str 1 2 3))
  #_"123"
  (ex-info :a)
  #_":a"
  (let [x {:a "1"}
        y "y"
        z :z]
    (ex-info [1 2 3 x y z {:a "1"}]))
  #_"1 2 3 {:a \"1\"} \"y\" :z {:a \"1\"}"
  (ex-info (err/expand-msg* [1 2 3 4 5 {1 2}]) {}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro try-wrap-ex
  "Execute throwable block of code.
   Catch exception and throw a new one with original as a cause.
   `ex-args` is a string or vector of args to `ex-info`."
  [ex-args & body]
  (let [ex-args (cond-> ex-args
                  (not (vector? ex-args)), (vector))
        ex-args (cond-> ex-args
                  (not (second ex-args)), (conj {}))]
    (clojure.core/assert (>= 2 (count ex-args))
                         (str "Too many ex-info args in " ex-args))
    `(try
       ~@body
       (catch Throwable ex#
         (throw (ex-info ~@ex-args ex#))
         nil))))

(comment
  (try
    (try-wrap-ex [["Wrap context" (str 1 2 3)] {:test true}]
      (throw (ex-info "Inner exception" {:inner true})))
    (catch Throwable ex
      (log-error ex)
      [(ex-message-all ex) (ex-data ex)]))
  #_["Wrap context \"123\" -> Inner exception" {:test true}]

  (try
    (try-wrap-ex "Wrap context"
      (throw (ex-info "Inner exception")))
    (catch Throwable ex
      (log-error ex)
      [(ex-message-all ex) (ex-data ex)]))
  #_["Wrap context -> Inner exception" {}])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro try-ignore
  "Execute throwable block of code.
   Catch any exceptions.
   Return `nil` on exception."
  [& body]
  `(try
     ~@body
     (catch Throwable _#
       nil)))

(comment
  (try-ignore :ok)
  (try-ignore
    (throw (ex-info "Test ex-info" {}))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro try-log-error
  "Execute throwable block of code.
   Catch any exceptions.
   Log error and return `nil` on exception.
   `error-log-args` is a string or vector of args to `log-error`."
  [error-log-args & body]
  (let [error-log-args (cond-> error-log-args
                         (not (vector? error-log-args)) (vector))]
    `(try
       ~@body
       (catch Throwable ex#
         (log-error ex# ~@error-log-args)
         nil))))

(comment
  (macroexpand '(try-log-error ["A" "B"] :body))
  (try-log-error ["Context message" 'log/error [1 "2" 3] {}]
    (throw (ex-info ["Test" "throw ex-info"] {:test-data "123"}))
    (throw (ex-info ["Test" "throw ex-info"]))
    (throw (ex-info "Test ex-info" {}))
    (throw (Exception. "Test exception")))
  #_"Context message log/error [1 \"2\" 3] {} -> Test throw ex-info ~//~ {:test-data \"123\"}"

  (try-log-error "Context message"
    (throw (ex-info ["Test" "throw ex-info"] {:test-data "123"}))
    (throw (ex-info ["Test" "throw ex-info"]))
    (throw (ex-info "Test ex-info" {}))
    (throw (Exception. "Test exception")))
  #_"Context message -> Test throw ex-info ~//~ {:test-data \"123\"}")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

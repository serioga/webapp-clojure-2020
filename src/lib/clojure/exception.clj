(ns lib.clojure.exception
  (:refer-clojure :exclude [ex-info])
  (:require [lib.clojure.error :as err]))

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

(defn ex-message-or-name
  "Returns the message attached to ex if ex is a Throwable.
   If message is null then class name is used."
  [^Throwable ex]
  (or (.getMessage ex)
      (.getCanonicalName (class ex))))

(defn ex-message-all
  "Collect single message from all nested exceptions."
  [^Throwable ex]
  (when (instance? Throwable ex)
    (loop [message (ex-message-or-name ex)
           cause (ex-cause ex)]
      (if cause
        (recur (str message \space _->_ \space (ex-message-or-name cause))
               (ex-cause cause))
        message))))

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

(defmacro ex-info
  "Same as `ex-info`, plus:
   - expands vector in `msg` to message string;
   - merges ex-data of `cause` to map `m`."
  ([msg] `(ex-info ~msg {}))
  ([msg data]
   `(clojure.core/ex-info (err/expand-msg* ~msg) ~data))
  ([msg data cause]
   `(let [cause# ~cause]
      (clojure.core/ex-info (err/expand-msg* ~msg)
                            (merge (ex-data cause#) ~data)
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
      [(ex-message-all ex) (ex-data ex)]))
  #_["Wrap context \"123\" -> Inner exception" {:test true}]

  (try
    (try-wrap-ex "Wrap context"
      (throw (ex-info "Inner exception")))
    (catch Throwable ex
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

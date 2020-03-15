(ns app.lib.util.exec
  "Execution control utility."
  (:refer-clojure :exclude [future, ex-info])
  (:require
    [app.lib.util.mdc :as mdc]
    [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)


(defn test-pred
  "Return `x` when `(pred x)` is truthy."
  [x pred]
  (when (pred x) x))


(defn opt-fn
  "If `v` is a function then invoke it else keep `v` as is.
   Useful to represent function parameters as value or function without arguments."
  [v]
  (if (fn? v) (v) v))

(comment
  (opt-fn 1)
  (opt-fn (constantly 1))
  (opt-fn nil)
  (opt-fn :kw))


(def _->_
  "Error message separator for nested exceptions etc."
  "->")


(defn print-str*
  "Similar to `print-str` but ignoring nils and empty strings."
  [& tokens]
  (if-some [tokens (seq tokens)]
    (-> ^StringBuilder (reduce (fn [^StringBuilder sb, ^Object x]
                                 (if-some [s (some-> x
                                                     (.toString)
                                                     (as-> s (when-not (.isEmpty s) s)))]
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


(defn ex-message-all
  "Collect single message from all nested exceptions."
  [^Throwable ex]
  (loop [msg (ex-message ex)
         cause (ex-cause ex)]
    (if cause
      (recur (print-str* msg _->_ (ex-message cause)) (ex-cause cause))
      msg)))

(comment
  (ex-message-all (clojure.core/ex-info "One" {:x :one}
                                        (clojure.core/ex-info "Two" {:x :two}
                                                              (clojure.core/ex-info "Three" {:x :three}))))
  #_"One -> Two -> Three")


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


(defn throwable?
  "Test if `x` is `Throwable`."
  [x]
  (instance? Throwable x))


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


(defn ex-data->log-str
  "Convert ex-data to string for logging."
  [ex-data]
  (when (and (map? ex-data)
             (pos? (count ex-data)))
    (print-str* "~//~" (pr-str ex-data))))


(defmacro log-error
  "Log error message."
  {:arglists '([msg*]
               [exception, msg*])}
  [ex & msg-tokens]
  (let [msg (vec msg-tokens)]
    `(let [ex# ~ex
           msg# (expand-msg* ~msg)
           msg# (if (throwable? ex#)
                  (print-str* (when (not= "" msg#) (print-str* msg# _->_))
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


(defmacro ex-info
  "Same as `ex-info`, plus:
   - expands vector in `msg` to message string;
   - merges ex-data of `cause` to `map`."
  ([msg] `(ex-info ~msg {}))
  ([msg map]
   `(clojure.core/ex-info (expand-msg* ~msg) ~map))
  ([msg map cause]
   `(let [cause# ~cause]
      (clojure.core/ex-info (expand-msg* ~msg)
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
  (ex-info (expand-msg* [1 2 3 4 5 {1 2}]) {}))


(defmacro try-wrap-ex
  "Execute throwable block of code.
   Catch exception and throw a new one with original as a cause.
   `ex-args` is a string or vector of args to `ex-info`."
  [ex-args & body]
  (let [ex-args (cond-> ex-args
                  (not (vector? ex-args)), (vector))
        ex-args (cond-> ex-args
                  (not (second ex-args)), (conj {}))]
    (assert (>= 2 (count ex-args))
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


(defmacro future
  "Same as `clojure.core/future` but preserving MDC context."
  [& body]
  `(let [ctx# (mdc/get-context-map)]
     (clojure.core/future
       (mdc/with-map ctx#
         ~@body))))


(defmacro thread-off
  "Execute background thread.
   Log error on exception.
   Returns nil."
  [context-msg & body]
  `(do
     (future (try-log-error ~context-msg ~@body))
     nil))


(defmacro thread-off!
  "Execute background thread.
   Don't care about exceptions!
   Returns nil."
  [& body]
  `(do
     (future ~@body)
     nil))

(comment
  (mdc/with-map {:outside 1}
    (future (log/error "inside")))

  (macroexpand '(thread-off :msg :body-a :body-b :body-c))

  (thread-off ["Test message"]
    (println "xxx")
    (throw (ex-info ["Test" "exception"])))

  (thread-off 'thread-off
    (println "xxx")
    (throw (ex-info ["Test" "exception"])))

  (thread-off!
    (throw (ex-info ["Test" "exception"]))
    (println "xxx")))

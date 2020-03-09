(ns app.lib.util.exec
  "Execution control utility."
  (:require
    [clojure.tools.logging :as log]
    [app.lib.util.logging-context :as logging-context])
  (:refer-clojure :exclude [future]))

(set! *warn-on-reflection* true)


(defn when-pred
  [pred x]
  (when (pred x) x))


(defn opt-fn
  "If `v` is a function then invoke it else keep `v` as is.
   Useful to represent function parameters as value or function without arguments."
  [v]
  (if (fn? v) (v) v))

#_(comment
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
    (->
      ^StringBuilder (reduce (fn [^StringBuilder sb, ^Object x]
                               (if-some [s (some-> x
                                                   (.toString)
                                                   (as-> s (when-not (.isEmpty s) s)))]
                                 (-> sb (.append " "), (.append s))
                                 sb))
                             (StringBuilder.) tokens)
      (.substring 1)
      (str))
    ""))

#_(comment
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

#_(comment
    (ex-message-all
      (ex-info "One" {:x :one} (ex-info "Two" {:x :two} (ex-info "Three" {:x :three}))))
    #_"One -> Two -> Three")


(defn ex-root-cause
  [^Throwable ex]
  (if-let [cause (ex-cause ex)]
    (recur cause)
    ex))

#_(comment
    (ex-root-cause
      (ex-info "One" {:x :one} (ex-info "Two" {:x :two} (ex-info "Three" {:x :three})))))


(defn throwable?
  [x]
  (instance? Throwable x))


(defmacro expand-msg*
  "Convert vector of message tokens to string.
   All symbolic tokens are wrapped with `pr-str`."
  [tokens]
  (cond
    (vector? tokens) `(print-str* ~@(let [wrap? (complement string?)
                                          wrap-pr (fn [v] (if (wrap? v) (list pr-str v), v))]
                                      (map wrap-pr tokens)))
    :else `(str ~tokens)))

#_(comment
    (expand-msg* ["a" "b" "c" 'd (do "e") {:f "f"}])
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
    (ex-info (expand-msg* [1 2 3 4 5 {1 2}]) {}))


(defn ex-data->log-str
  [ex-data]
  (when (and (map? ex-data), (pos? (count ex-data)))
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
                  (print-str*
                    (when (not= "" msg#) (print-str* msg# _->_))
                    (ex-message-all ex#)
                    (some-> ex# ex-data ex-data->log-str))
                  msg#)]
       (log/error ex# msg#))))

#_(comment
    (log-error
      (ex-info "One" {:x :one :y "y"} (ex-info "Two" {:x :two} (ex-info "Three" {:x :three})))
      "A" "B" "C" (str "D" "E" "F"))
    #_"A B C \"DEF\" -> One -> Two -> Three ~//~ {:x :one}"
    (log-error
      (ex-info "One" {:x :one} (ex-info "Two" {:x :two} (ex-info "Three" {:x :three})))
      ["A" "B" "C" (str "D" "E" "F")])
    #_"[\"A\" \"B\" \"C\" \"DEF\"] -> One -> Two -> Three ~//~ {:x :one}"
    (log-error
      (ex-info "One" {:x :one} (ex-info "Two" {:x :two} (ex-info "Three" {:x :three}))))
    #_"One -> Two -> Three ~//~ {:x :one}")


(defmacro throw-ex-info
  "Throw ex-info.
   Create message from separate tokens similar to `print`.
   Attach last item of `msg+data` as ex-data if it is inline map.
   Accept optional exception to rethrow as first argument.
   Merge ex-data of the optional exception."
  {:arglists '([msg*, ex-data-map?]
               [exception, msg*, ex-data-map?])}
  [ex & msg+data]
  (let [msg (vec msg+data)
        data (peek msg)
        data (when (map? data) data)
        msg (cond-> msg data (pop))
        data (or data {})]
    `(let [ex# ~ex
           msg# (expand-msg* ~msg)]
       (if (throwable? ex#)
         (throw (ex-info msg# (merge (ex-data ex#) ~data) ex#))
         (throw (ex-info (print-str* ex# msg#) ~data))))))

#_(comment
    (throw-ex-info "a" "b" "c" (inc 1))
    (throw-ex-info "a" "b" "c" (inc 1) {:test (inc 2)})
    (throw-ex-info (ex-info "Cause" {}) "a" "b" "c" (inc 1) {:test (inc 2)})
    (try
      (throw-ex-info "a" "b" "c" (inc 1) {:test (inc 2)})
      (catch Throwable ex
        (ex-message-all ex)))
    #_"a b c 2"
    (try
      (throw-ex-info (ex-info "Cause" {}) "a" "b" "c" (inc 1) {:test (inc 2)})
      (catch Throwable ex
        (ex-message-all ex)))
    #_"a b c 2 -> Cause"
    (macroexpand '(throw-ex-info "a" "b" "c" (inc 1) {:test (inc 2)})))


(defmacro try-wrap-ex
  "Execute throwable block of code.
   Catch exception and throw a new one with original as a cause."
  {:arglists '([[msg* ex-data-map?] body*]
               [msg body*])}
  [msg+data & body]
  (let [msg+data (cond-> msg+data
                   (not (vector? msg+data)) (vector))]
    `(try
       ~@body
       (catch Throwable ex#
         (throw-ex-info ex# ~@msg+data)
         nil))))

#_(comment
    (try
      (try-wrap-ex ["Wrap context" (str 1 2 3) {:test true}]
        (throw-ex-info "Inner exception"))
      (catch Throwable ex
        (log-error ex)
        [(ex-message-all ex) (ex-data ex)]))
    #_["Wrap context \"123\" -> Inner exception" {:test true}]

    (try
      (try-wrap-ex "Wrap context"
        (throw-ex-info "Inner exception"))
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

#_(comment
    (try-ignore :ok)
    (try-ignore
      (throw (ex-info "Test ex-info" {}))))


(defmacro try-log-error
  "Execute throwable block of code.
   Catch any exceptions.
   Log error and return `nil` on exception."
  {:arglists '([[msg* _], body*]
               [msg body*])}
  [msg+data & body]
  (let [msg+data (cond-> msg+data
                   (not (vector? msg+data)) (vector))]
    `(try
       ~@body
       (catch Throwable ex#
         (log-error ex# ~@msg+data)
         nil))))

#_(comment
    (macroexpand '(try-log-error ["A" "B"] :body))
    (try-log-error ["Context message" 'log/error [1 "2" 3] {}]
      (throw-ex-info "Test" "throw-ex-info" {:test-data "123"})
      (throw-ex-info "Test" "throw-ex-info")
      (throw (ex-info "Test ex-info" {}))
      (throw (Exception. "Test exception")))
    #_"Context message log/error [1 \"2\" 3] {} -> Test throw-ex-info ~//~ {:test-data \"123\"}"

    (try-log-error "Context message"
      (throw-ex-info "Test" "throw-ex-info" {:test-data "123"})
      (throw-ex-info "Test" "throw-ex-info")
      (throw (ex-info "Test ex-info" {}))
      (throw (Exception. "Test exception")))
    #_"Context message -> Test throw-ex-info ~//~ {:test-data \"123\"}")


(defmacro future
  [& body]
  `(let [ctx# (logging-context/get-logging-context)]
     (clojure.core/future
       (logging-context/with-logging-context ctx#
         ~@body))))


(defmacro thread-off
  "Execute background thread.
   Log error on exception.
   Returns nil."
  [context-msg & body]
  `(do
     (future
       (try-log-error ~context-msg ~@body))
     nil))


(defmacro thread-off!
  "Execute background thread.
   Don't care about exceptions!
   Returns nil."
  [& body]
  `(do
     (future
       ~@body)
     nil))

#_(comment
    (logging-context/with-logging-context {:outside 1}
      (future
        (log/error "inside")))

    (macroexpand
      '(thread-off :msg :body-a :body-b :body-c))

    (thread-off ["Test message"]
      (println "xxx")
      (throw-ex-info "Test" "exception"))

    (thread-off 'thread-off
      (println "xxx")
      (throw-ex-info "Test" "exception"))

    (thread-off!
      (throw-ex-info "Test" "exception")
      (println "xxx")))

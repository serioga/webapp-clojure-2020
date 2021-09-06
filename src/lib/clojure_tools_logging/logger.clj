(ns lib.clojure-tools-logging.logger
  "Logging macros using explicit logger instance."
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as impl]
            [lib.clojure-string.core :as string]
            [lib.clojure.exception :as ex]
            [lib.clojure.print :as print]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-logger
  "Returns an implementation-specific Logger by name."
  [logger-name]
  (impl/get-logger log/*logger-factory* logger-name))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro log-enabled
  "Evaluates and logs a message only if the specified level is enabled. See log*
   for more details."
  ([logger level message] `(log-enabled ~logger ~level nil ~message))
  ([logger level throwable message]
   `(if (impl/enabled? ~logger ~level)
      (log/log* ~logger ~level ~throwable ~message))))

(comment
  (let [logger (impl/get-logger log/*logger-factory* "test")
        s "4" n nil]
    (log-enabled logger :info (print/p-str 1 2 3 "X" s n :k))
    (log-enabled logger :error (print/p-str 1 2 3 "X" s n :k))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- str-ex-data
  "Converts all nested ex-data to string for logging."
  [ex]
  (loop [s nil ex ex]
    (if ex
      (recur (if-let [d (not-empty (ex-data ex))]
               (-> ^String (or s "~//~")
                   (.concat " ")
                   (.concat (str d)))
               s)
             (.getCause ^Throwable ex))
      s)))

(defn str-throwable
  "Returns message string for throwable."
  [throwable message]
  (let [message (string/not-empty message)]
    (string/join-not-empty \space [message, (when message ex/_->_)
                                   (ex/ex-message-all throwable)
                                   (str-ex-data throwable)])))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro info
  "Info level logging."
  [logger message]
  `(log-enabled ~logger :info ~message))

(defmacro debug
  "Debug level logging."
  [logger message]
  `(log-enabled ~logger :debug ~message))

(defmacro error
  "Error level logging."
  [logger message]
  `(log-enabled ~logger :error ~message))

(defmacro log-throwable
  "Error level logging of the throwable."
  [logger throwable message]
  `(let [throwable# ~throwable]
     (log/log* ~logger :error throwable# (str-throwable throwable# ~message))))

(comment
  (let [logger (get-logger "test")
        throwable (ex-info "Exception" {:error true} (ex-info "Cause" {:cause true}))
        message (print/p-str 1 2 3 (str "4") "X")]
    (info logger message)
    (debug logger message)
    (error logger message)
    (log-throwable logger throwable nil)
    (log-throwable logger throwable message)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

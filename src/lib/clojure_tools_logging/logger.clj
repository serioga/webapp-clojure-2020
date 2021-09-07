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
  "Converts all nested ex-data to the logging string."
  [throwable]
  (loop [sb (StringBuilder.), nothing true, throwable throwable]
    (if throwable
      (let [data (not-empty (ex-data throwable))]
        (recur (cond-> sb data (-> (cond-> nothing (.append "~//~"))
                                   (.append \space)
                                   (.append (str data))))
               (and nothing (not data))
               (.getCause ^Throwable throwable)))
      (when (pos? (.length sb))
        (.toString sb)))))

(defn str-throwable
  "Returns message string for the throwable."
  [throwable message]
  (let [data (str-ex-data throwable)]
    (cond-> ^String (ex/ex-message-all throwable (-> (str message) (string/not-empty)))
      data (-> (.concat " ") (.concat data)))))

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

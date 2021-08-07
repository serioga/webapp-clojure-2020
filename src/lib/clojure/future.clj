(ns lib.clojure.future
  (:refer-clojure :exclude [future])
  (:require [lib.clojure.exception :as ex]
            [lib.slf4j.mdc :as mdc]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro future
  "Same as `clojure.core/future` but preserving MDC context."
  [& body]
  `(let [ctx# (mdc/get-context-map)]
     (clojure.core/future
       (mdc/with-map ctx#
         ~@body))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro thread-off
  "Execute background thread.
   Log error on exception.
   Returns nil."
  [context-msg & body]
  `(do
     (future (ex/try-log-error ~context-msg ~@body))
     nil))

(defmacro thread-off!
  "Execute background thread.
   Don't care about exceptions!
   Returns nil."
  [& body]
  `(do
     (future ~@body)
     nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns lib.clojure.future
  (:refer-clojure :exclude [future])
  (:require [lib.slf4j.mdc :as mdc]))

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

(ns lib.clojure.future
  (:refer-clojure :exclude [future])
  (:import (org.slf4j MDC)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro future
  "Same as `clojure.core/future` but preserving MDC context map."
  [& body]
  `(let [cm# (MDC/getCopyOfContextMap)]
     (clojure.core/future
       (some-> cm# (MDC/setContextMap))
       (try
         ~@body
         (finally (MDC/clear))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

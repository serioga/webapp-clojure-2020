(ns lib.ring-middleware.logging-context
  (:require [lib.slf4j.mdc :as mdc])
  (:import (java.util UUID)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-logging-context
  "Wrap handler with MDC logging context.
   `req-ks` is a sequence of request keys to be added in the logging context."
  ([handler] (wrap-logging-context handler nil))
  ([handler request-keys]
   (if-some [ks (seq request-keys)]
     (fn [request]
       (mdc/with-keys request ks
         (mdc/with-map {:request-id (UUID/randomUUID)}
           (handler request))))
     (fn [request]
       (mdc/with-map {:request-id (UUID/randomUUID)}
         (handler request))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

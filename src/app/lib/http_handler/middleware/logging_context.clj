(ns app.lib.http-handler.middleware.logging-context
  (:require
    [app.lib.util.mdc :as mdc])
  (:import
    (java.util UUID)))

(set! *warn-on-reflection* true)


(defn wrap-logging-context
  "Wrap handler with MDC logging context."
  [handler]
  (fn [request]
    (mdc/with-keys request [:server-name :route-tag :session]
      (mdc/with-map {:request-id (UUID/randomUUID)}
        (handler request)))))

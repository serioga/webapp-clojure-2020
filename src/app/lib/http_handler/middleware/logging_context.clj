(ns app.lib.http-handler.middleware.logging-context
  (:require
    [app.lib.util.mdc :as mdc]
    [app.lib.util.perf :as p])
  (:import
    (java.util UUID)))

(set! *warn-on-reflection* true)


(defn wrap-logging-context
  "Wrap handler with MDC logging context."
  [handler]
  (fn [request]
    (mdc/wrap-with-map (-> request
                           (p/fast-select-keys [:server-name :route-tag :session])
                           (p/fast-assoc :request-id (UUID/randomUUID)))
      (handler request))))

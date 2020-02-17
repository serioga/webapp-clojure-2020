(ns app.lib.http-handler.middleware.logging-context
  (:require
    [app.lib.util.perf :as perf]
    [app.lib.util.logging-context :as logging-context])
  (:import
    (java.util UUID)))

(set! *warn-on-reflection* true)


(defn wrap-logging-context [handler]
  (fn [request]
    (logging-context/with-logging-context
      (-> request
        (perf/fast-select-keys [:server-name :route-tag :session])
        (perf/fast-assoc :request-id (UUID/randomUUID)))
      (handler request))))

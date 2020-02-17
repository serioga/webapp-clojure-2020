(ns app.lib.http-handler.middleware.debug-response
  (:require
    [app.lib.util.perf :as perf]
    [app.lib.util.ring :as ring-util]
    [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)


(defn response-description
  [request response time-millis]
  (let [{:keys [route-tag
                params
                request-method
                server-name]} request
        {:keys [status]} response
        uri (ring-util/request-uri request)]
    (perf/inline-str
      "HTTP " status " < "
      server-name " " route-tag " " request-method " " uri " " params
      " (" time-millis " ms)")))


(defn session-update-description [response]
  (when-let [session (:session response)]
    (if (:recreate (meta session))
      (perf/inline-str "Recreate :session " session)
      (perf/inline-str "Update :session " session))))


(defn flash-update-description [response]
  (when-let [flash (:flash response)]
    (perf/inline-str "Set :flash " flash)))


(defn cookies-update-description [response]
  (when-let [cookies (:cookies response)]
    (perf/inline-str "Set :cookies " cookies)))


(defn wrap-debug-response [handler]
  (fn [request]
    (let [start-millis (System/currentTimeMillis)
          response (handler request)
          time-millis (- (System/currentTimeMillis) start-millis)]
      (log/debug (response-description request response time-millis))
      (some->
        (session-update-description response)
        (log/debug))
      (some->
        (flash-update-description response)
        (log/debug))
      (some->
        (cookies-update-description response)
        (log/debug))
      response)))

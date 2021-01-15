(ns app.lib.http-handler.middleware.debug-response
  (:require
    [app.lib.util.perf :as p]
    [clojure.tools.logging :as log]
    [lib.ring-util.request :as ring-request]))

(set! *warn-on-reflection* true)


(defn- response-description
  [request response time-millis]
  (let [{:keys [route-tag
                params
                request-method
                server-name]} request
        {:keys [status]} response
        uri (ring-request/request-uri request)]
    (p/inline-str "HTTP " status " < "
                  server-name " " route-tag " " request-method " " uri " " params
                  " (" time-millis " ms)")))


(defn- session-update-description [response]
  (when-let [session (:session response)]
    (if (:recreate (meta session))
      (p/inline-str "Recreate :session " session)
      (p/inline-str "Update :session " session))))


(defn- flash-update-description [response]
  (when-let [flash (:flash response)]
    (p/inline-str "Set :flash " flash)))


(defn- cookies-update-description [response]
  (when-let [cookies (:cookies response)]
    (p/inline-str "Set :cookies " cookies)))


(defn wrap-debug-response
  "Wrap handler with debug logging."
  [handler]
  (fn [request]
    (let [start-millis (System/currentTimeMillis)
          response (handler request)
          time-millis (- (System/currentTimeMillis) start-millis)]
      (log/debug (response-description request response time-millis))
      (some-> (session-update-description response)
              (log/debug))
      (some-> (flash-update-description response)
              (log/debug))
      (some-> (cookies-update-description response)
              (log/debug))
      response)))

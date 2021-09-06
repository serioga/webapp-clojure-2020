(ns lib.ring-middleware.debug-response
  (:require [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.perf :as p]
            [lib.ring-util.request :as ring-request]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- response-description
  [request response time-millis]
  (let [{:keys [route-tag
                params
                request-method
                server-name]} request
        {:keys [status]} response
        uri (ring-request/request-uri request)]
    (p/inline-str "HTTP " status " < "
                  server-name " " route-tag (when route-tag " ") request-method " " uri " " params
                  " (" time-millis " ms)")))

(defn- session-update-description
  [response]
  (when-let [session (:session response)]
    (if (:recreate (meta session))
      (p/inline-str "Recreate :session " session)
      (p/inline-str "Update :session " session))))

(defn- flash-update-description
  [response]
  (when-let [flash (:flash response)]
    (p/inline-str "Set :flash " flash)))

(defn- cookies-update-description
  [response]
  (when-let [cookies (:cookies response)]
    (p/inline-str "Set :cookies " cookies)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-debug-response
  "Wrap handler with debug logging."
  [handler]
  (fn [request]
    (let [start-millis (System/currentTimeMillis)
          response (handler request)
          time-millis (- (System/currentTimeMillis) start-millis)]
      (logger/debug logger (response-description request response time-millis))
      (some->> (session-update-description response) (logger/debug logger))
      (some->> (flash-update-description response) (logger/debug logger))
      (some->> (cookies-update-description response) (logger/debug logger))
      response)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

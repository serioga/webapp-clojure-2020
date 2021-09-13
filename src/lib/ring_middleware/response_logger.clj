(ns lib.ring-middleware.response-logger
  (:require [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.perf :as p]
            [lib.ring-util.request :as ring-request]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- response-description
  [request response time-millis]
  (let [{:keys [server-name, request-method, form-params, route-tag]} request
        {:keys [status, response-logger/log-response]} response
        uri (ring-request/request-uri request)
        form-params (not-empty form-params)
        log-response (or log-response (some-> response :headers (get "Content-Type")))]
    (p/inline-str "HTTP " status " >> "
                  route-tag (when route-tag " ")
                  request-method " " server-name " " uri
                  (when form-params " ") form-params
                  (when log-response "   >>   ") log-response
                  " | " time-millis " ms")))

(defn- session-update-description
  [response]
  (when-let [session (:session response)]
    (if (:recreate (meta session))
      (.concat "Recreate :session " (pr-str session))
      (.concat "Update :session " (pr-str session)))))

(defn- flash-update-description
  [response]
  (when-let [flash (:flash response)]
    (.concat "Set :flash " (pr-str flash))))

(defn- cookies-update-description
  [response]
  (when-let [cookies (:cookies response)]
    (.concat "Set :cookies " (pr-str cookies))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-response-logger
  "Wrap handler with response logging."
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

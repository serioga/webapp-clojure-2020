(ns lib.ring-middleware.error-exception
  (:require [lib.clojure.core :as e]
            [lib.ring-util.response :as ring-response]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- response-error-exception
  [^Throwable ex, dev-mode?]
  (let [status 500
        message (str "[HTTP " status "] "
                     (e/ex-message-all ex)
                     (when dev-mode?
                       (str "\n\n" "---" "\n"
                            "Default exception handler, dev mode."
                            "\n\n"
                            (prn-str ex))))]
    (ring-response/plain-text message status)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-error-exception
  "Wrap handler with exception handler."
  [handler, dev-mode?]
  (fn [request]
    (try
      (handler request)
      (catch Throwable ex
        (e/log-error ex)
        (response-error-exception ex, dev-mode?)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns lib.ring-middleware.error-exception
  (:require [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]
            [lib.ring-util.response :as ring.response']))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- response-error-exception
  [throwable, dev-mode]
  (let [status 500
        message (str "[HTTP " status "] "
                     (c/ex-message-all throwable)
                     (when dev-mode
                       (str "\n\n" "---" "\n"
                            "Default exception handler, dev mode."
                            "\n\n"
                            (prn-str throwable))))]
    (ring.response'/plain-text message status)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-error-exception
  "Wrap handler with exception handler."
  [handler, dev-mode]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (logger/log-throwable e "Handle HTTP request")
        (response-error-exception e, dev-mode)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

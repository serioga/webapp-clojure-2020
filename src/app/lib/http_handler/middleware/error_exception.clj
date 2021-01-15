(ns app.lib.http-handler.middleware.error-exception
  (:require
    [app.lib.util.ring :as ring-util]
    [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)


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
    (ring-util/plain-text-response message status)))


(defn wrap-error-exception
  "Wrap handler with exception handler."
  [handler, dev-mode?]
  (fn [request]
    (try
      (handler request)
      (catch Throwable ex
        (e/log-error ex)
        (response-error-exception ex, dev-mode?)))))

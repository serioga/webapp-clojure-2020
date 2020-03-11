(ns app.lib.http-handler.middleware.exceptions
  (:require
    [app.lib.util.exec :as exec]
    [app.lib.util.ring :as ring-util]))

(set! *warn-on-reflection* true)


(defn response
  [^Throwable ex, dev-mode?]
  (let [status 500
        message (str "[HTTP " status "] "
                     (exec/ex-message-all ex)
                     (when dev-mode?
                       (str "\n\n" "---" "\n"
                            "Default exception handler, dev mode."
                            "\n\n"
                            (prn-str ex))))]
    (ring-util/plain-text-response message status)))


(defn wrap-exceptions
  [handler, dev-mode?]
  (fn [request]
    (try
      (handler request)
      (catch Throwable ex
        (exec/log-error ex)
        (response ex, dev-mode?)))))

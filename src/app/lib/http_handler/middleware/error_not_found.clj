(ns app.lib.http-handler.middleware.error-not-found
  (:require
    [app.lib.util.ring :as ring-util]
    [clojure.pprint :as pprint]
    [ring.util.request :as ring-request]))

(set! *warn-on-reflection* true)


(defn- response-error-not-found
  [request, dev-mode?]
  (-> (str "[HTTP 404] Resource not found.\n\n"
           "URL: "
           (ring-request/request-url request)
           (when dev-mode?
             (str
               "\n\n" "---" "\n"
               "Default not-found handler, dev mode."
               "\n\n"
               (with-out-str (pprint/pprint request)))))
      (ring-util/plain-text-response 404)))


(defn wrap-error-not-found
  "Wrap handler with middleware replacing `nil` response with default."
  [handler, dev-mode?]
  (fn [request] (or (handler request)
                    (response-error-not-found request, dev-mode?))))

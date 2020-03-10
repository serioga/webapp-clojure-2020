(ns app.lib.http-handler.middleware.default-not-found
  (:require
    [app.lib.util.ring :as ring-util]
    [clojure.pprint :as pprint]
    [ring.util.request :as ring-request]))

(set! *warn-on-reflection* true)


(defn default-not-found-response
  "Default handler for resources with empty route-tag or empty response."
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


(defn wrap-default-not-found
  [handler, dev-mode?]
  (fn [request] (or (handler request)
                    ; response not-found if webapp response is empty
                    (default-not-found-response request, dev-mode?))))

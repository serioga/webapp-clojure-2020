(ns app.webapp.ring-handler
  "Ring-based definition for request-response handling."
  (:require [lib.clojure.core :as c]
            [lib.ring-middleware.error-exception :as error-exception]
            [lib.ring-middleware.error-not-found :as error-not-found]
            [lib.ring-middleware.response-logger :as debug-response]
            [lib.ring-middleware.route-tag-reitit :as route-tag]
            [lib.slf4j.mdc :as mdc]
            [reitit.core :as reitit]
            [ring.middleware.defaults :as defaults])
  (:import (java.util UUID)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn collect-routes
  "Returns vector of reitit routes defined by `route-path` multimethod."
  [route-path]
  (->> (keys (methods route-path))
       (group-by route-path)
       (sort-by first)
       (mapv (fn [[path tags]]
               (c/assert (= 1 (count tags)) (c/pr-str* "Duplicate route-path" path "for tags" tags))
               [path (first tags)]))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- wrap-mdc
  [handler]
  (fn [request]
    (with-open [_ (mdc/put-closeable "hostname" (request :server-name))
                _ (mdc/put-closeable "route-tag" (some-> (request :route-tag) (str)))
                _ (mdc/put-closeable "session" (some-> (request :session) (str)))
                _ (mdc/put-closeable "request-id" (.toString (UUID/randomUUID)))]
      (handler request))))

(defn webapp-http-handler
  "Build HTTP server handler for webapp with common middleware."
  [http-handler, routes, {:keys [dev-mode]}]
  (-> http-handler
      (error-not-found/wrap-error-not-found dev-mode)
      (debug-response/wrap-response-logger)
      (wrap-mdc)
      (route-tag/wrap-route-tag (reitit/router routes))
      (defaults/wrap-defaults (-> defaults/site-defaults
                                  (assoc-in [:security :anti-forgery] false)
                                  (assoc-in [:security :frame-options] false)
                                  (dissoc :session)))
      (error-exception/wrap-error-exception dev-mode)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

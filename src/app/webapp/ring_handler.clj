(ns app.webapp.ring-handler
  "Ring-based definition for request-response handling."
  (:require [lib.ring-middleware.debug-response :as debug-response]
            [lib.ring-middleware.error-exception :as error-exception]
            [lib.ring-middleware.error-not-found :as error-not-found]
            [lib.ring-middleware.route-tag-reitit :as route-tag]
            [lib.slf4j.mdc :as mdc]
            [reitit.core :as reitit]
            [ring.middleware.defaults :as ring-defaults])
  (:import (java.util UUID)))

(set! *warn-on-reflection* true)

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
      (debug-response/wrap-debug-response)
      (wrap-mdc)
      (route-tag/wrap-route-tag (reitit/router routes))
      (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                       (assoc-in [:security :anti-forgery] false)
                                       (assoc-in [:security :frame-options] false)
                                       (dissoc :session)))
      (error-exception/wrap-error-exception dev-mode)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.web-example.ring-handler.core
  "Ring-based definition for request-response handling."
  (:require [lib.ring-middleware.debug-response :as debug-response]
            [lib.ring-middleware.error-exception :as error-exception]
            [lib.ring-middleware.error-not-found :as error-not-found]
            [lib.ring-middleware.logging-context :as logging-context]
            [lib.ring-middleware.route-tag-reitit :as route-tag]
            [reitit.core :as reitit]
            [ring.middleware.defaults :as ring-defaults]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn webapp-http-handler
  "Build HTTP server handler for webapp with common middleware."
  [http-handler, routes, {:keys [dev-mode?]}]
  (-> http-handler
      (error-not-found/wrap-error-not-found dev-mode?)
      (debug-response/wrap-debug-response)
      (logging-context/wrap-logging-context [:server-name :route-tag :session])
      (route-tag/wrap-route-tag (reitit/router routes))
      (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                       (assoc-in [:security :anti-forgery] false)
                                       (assoc-in [:security :frame-options] false)
                                       (dissoc :session)))
      (error-exception/wrap-error-exception dev-mode?)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

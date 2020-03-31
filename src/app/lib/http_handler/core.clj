(ns app.lib.http-handler.core
  "Ring-based definition for request-response handling."
  (:require
    [app.lib.http-handler.middleware.debug-response :as debug-response]
    [app.lib.http-handler.middleware.error-exception :as error-exception]
    [app.lib.http-handler.middleware.error-not-found :as error-not-found]
    [app.lib.http-handler.middleware.logging-context :as logging-context]
    [app.lib.http-handler.middleware.route-tag :as route-tag]
    [reitit.core :as reitit]
    [ring.middleware.defaults :as ring-defaults]))

(set! *warn-on-reflection* true)


(defn webapp-http-handler
  "Build HTTP server handler for webapp with common middleware."
  [http-handler, routes, {:keys [dev-mode?]}]
  (-> http-handler
      (error-not-found/wrap-error-not-found dev-mode?)
      (debug-response/wrap-debug-response)
      (logging-context/wrap-logging-context)
      (route-tag/wrap-route-tag (reitit/router routes))
      (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                       (assoc-in [:security :anti-forgery] false)
                                       (assoc-in [:security :frame-options] false)
                                       (dissoc :session)))
      (error-exception/wrap-error-exception dev-mode?)))

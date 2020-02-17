(ns app.lib.http-handler.core
  "Ring-based definition for request-response handling."
  (:require
    [app.lib.http-handler.middleware.debug-response :as debug-response]
    [app.lib.http-handler.middleware.default-not-found :as default-not-found]
    [app.lib.http-handler.middleware.exceptions :as exceptions]
    [app.lib.http-handler.middleware.logging-context :as logging-context]
    [app.lib.http-handler.middleware.route-tag :as route-tag]
    [reitit.core :as reitit]
    [ring.middleware.defaults :as ring-defaults]))

(set! *warn-on-reflection* true)


(defn webapp-http-handler
  [http-handler, routes, {:keys [dev-mode?]}]
  (-> http-handler
    (default-not-found/wrap-default-not-found dev-mode?)
    (debug-response/wrap-debug-response)
    (logging-context/wrap-logging-context)
    (route-tag/wrap-route-tag (reitit/router routes))
    (ring-defaults/wrap-defaults
      (-> ring-defaults/site-defaults
        (assoc-in [:security :anti-forgery] false)
        (assoc-in [:security :frame-options] false)
        (dissoc :session)))
    (exceptions/wrap-exceptions dev-mode?)))

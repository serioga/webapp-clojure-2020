(ns app.lib.http-handler.middleware.session
  (:require
    [immutant.web.middleware :as immutant]
    [ring.middleware.flash :as ring-flash]))

(set! *warn-on-reflection* true)


(defn wrap-session
  [handler]
  (-> handler
      ring-flash/wrap-flash
      (immutant/wrap-session {:cookie-attrs {:http-only true}})))

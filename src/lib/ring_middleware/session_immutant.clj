(ns lib.ring-middleware.session-immutant
  (:require [immutant.web.middleware :as immutant]
            [ring.middleware.flash :as flash]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-session
  "Wrap handler with immutant-web session middleware."
  [handler]
  (-> handler
      (flash/wrap-flash)
      (immutant/wrap-session {:cookie-attrs {:http-only true}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

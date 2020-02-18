(ns app.web-homepage.core
  (:require
    ; route handlers
    [app.web-homepage.handler.index]
    [app.web-homepage.handler.index-lang]
    ; react components
    [app.web-homepage.config.react-components]
    ; imports
    [app.lib.http-handler.core :as http-handler]
    [app.web-homepage.impl.handler :as handler]))

(set! *warn-on-reflection* true)


(defn homepage-routes
  []
  [["/" :route/index]
   ["/:lang" :route/index-lang]])


(defn homepage-http-handler
  [config]
  (http-handler/webapp-http-handler handler/homepage-handler, (homepage-routes), config))

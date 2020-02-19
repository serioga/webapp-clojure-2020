(ns app.web-example.core
  (:require
    ; route handlers
    [app.web-example.handler.index]
    [app.web-example.handler.index-lang]
    ; react components
    [app.web-example.config.react-components]
    ; imports
    [app.lib.http-handler.core :as http-handler]
    [app.web-example.impl.handler :as handler]))

(set! *warn-on-reflection* true)


(defn example-routes
  []
  [["/" :route/index]
   ["/:lang" :route/index-lang]])


(defn example-http-handler
  [config]
  (http-handler/webapp-http-handler handler/example-handler, (example-routes), config))

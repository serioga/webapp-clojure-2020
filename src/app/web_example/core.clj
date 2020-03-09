(ns app.web-example.core
  (:require ; route handlers
    [app.web-example.handler.example-database]
    [app.web-example.handler.example-path-param]
    [app.web-example.handler.example-react]
    [app.web-example.handler.index])
  (:require ; react components
    [app.web-example.config.react-components])
  (:require ; imports
    [app.lib.http-handler.core :as http-handler]
    [app.web-example.impl.handler :as handler]))

(set! *warn-on-reflection* true)


(defn example-routes
  []
  [["/" :route/index]
   ["/example-database" :route/example-database]
   ["/example-react" :route/example-react]
   ["/example-path-param/:name" :route/example-path-param]])


(defn example-http-handler
  [config]
  (http-handler/webapp-http-handler handler/example-handler, (example-routes), config))

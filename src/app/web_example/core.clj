(ns app.web-example.core
  ;; React components
  (:require [app.rum.core])
  ;; Imports
  (:require [app.web-example.impl.handler :as handler]
            [app.web-example.ring-handler.core :as http-handler]
            [lib.clojure.ns :as ns]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.web-example.handler._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- example-routes
  []
  [["/" :route/index]
   ["/example-database" :route/example-database]
   ["/example-react" :route/example-react]
   ["/example-path-param/:name" :route/example-path-param]])

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn example-http-handler
  "HTTP server handler for `example` webapp."
  [config]
  (http-handler/webapp-http-handler handler/example-handler, (example-routes), config))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

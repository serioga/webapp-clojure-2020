(ns app.-example-webapp-.core
  ;; React components
  (:require [app.rum.core])
  ;; Imports
  (:require [app.-example-webapp-.impl.handler :as handler]
            [app.webapp.ring-handler :as ring-handler]
            [lib.clojure.ns :as ns]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.-example-webapp-.handler._)

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
  (ring-handler/webapp-http-handler handler/example-handler, (example-routes), config))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

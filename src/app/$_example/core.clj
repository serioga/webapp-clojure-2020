(ns app.$-example.core
  (:require [app.$-example.impl.handler :as handler]
            [app.rum.core #_"React components"]
            [app.webapp.ring-handler :as ring-handler]
            [lib.clojure.ns :as ns]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'app.$-example.handler._)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- example-routes
  []
  (ring-handler/collect-routes handler/route-path))

(comment
  (example-routes))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn example-http-handler
  "HTTP server handler for `example` webapp."
  [config]
  (ring-handler/webapp-http-handler handler/example-handler, (example-routes), config))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

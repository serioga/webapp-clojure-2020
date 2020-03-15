(ns app.app-system.service.webapp-http-handler
  (:require
    [app.lib.util.exec :as e]
    [app.web-example.core :as example]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


(defmulti webapp-http-handler
  "Provide webapp server handler by :name from config."
  :name)


(defmethod webapp-http-handler :default
  [{:keys [name]}]
  (e/throw-ex-info "Webapp handler is not found for name" name))


(defmethod webapp-http-handler "example"
  [config]
  (example/example-http-handler config))


(defmethod ig/init-key :app-system.service/webapp-http-handler
  [_ {:keys [name, hosts] :as config}]
  (if (seq hosts)
    {:name name
     :handler (webapp-http-handler config)
     :options {:virtual-host (vec hosts)}}
    {:name name
     :enabled? false}))

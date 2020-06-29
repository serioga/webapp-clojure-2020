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
  (throw (e/ex-info ["Webapp handler is not found for name" name])))


(defmethod webapp-http-handler "example"
  [config]
  (example/example-http-handler config))


(defmethod ig/init-key :app-system.service/webapp-http-handler
  [_ {:keys [hosts, enabled?] :or {enabled? true} :as config}]
  (cond-> config
    enabled? (-> (assoc :handler (webapp-http-handler config)
                        :options (cond-> {}
                                   (seq hosts) (assoc :virtual-host (vec hosts)))))))

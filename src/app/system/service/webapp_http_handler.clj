(ns app.system.service.webapp-http-handler
  (:require [app.$example$-webapp.core :as example]
            [integrant.core :as ig]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti webapp-http-handler
  "Provide webapp server handler by :name from config."
  :name)

(defmethod webapp-http-handler :default
  [{webapp-name :name}]
  (throw (Exception. (c/pr-str* "Webapp handler is not found for name" webapp-name) nil)))

(c/add-method webapp-http-handler "example"
              example/example-http-handler)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.service/webapp-http-handler
  [_ {:keys [hosts, system-is-enabled] :or {system-is-enabled true} :as config}]
  (cond-> config
    system-is-enabled (-> (assoc :handler (webapp-http-handler config)
                                 :options (cond-> {}
                                            (seq hosts) (assoc :virtual-host (vec hosts)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

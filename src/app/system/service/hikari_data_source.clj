(ns app.system.service.hikari-data-source
  (:require [integrant.core :as ig]
            [lib.hikari-cp.data-source :as data-source])
  (:import (com.p6spy.engine.spy P6DataSource)
           (java.io Closeable)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init-data-source
  [options]
  (let [ds (data-source/create-data-source options)
        spy-wrapped-ds (P6DataSource. ds)]
    spy-wrapped-ds))

(defn- close-data-source!
  [^P6DataSource spy-wrapped-ds]
  (.close ^Closeable (.unwrap spy-wrapped-ds Closeable)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.service/hikari-data-source
  [_ {:keys [dev-mode] :as options}]
  (init-data-source (-> {:minimum-idle 1
                         :maximum-pool-size 10
                         :connection-timeout 5000
                         :leak-detection-threshold 30000}
                        (cond-> dev-mode (assoc :max-lifetime 300000
                                                :idle-timeout 60000))
                        (merge options))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :app.system.service/hikari-data-source
  [_ data-source]
  (close-data-source! data-source))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.system.service.hikari-data-source
  (:require
    [app.lib.database.hikari-data-source :as data-source]
    [app.lib.util.exec :as e]
    [clojure.tools.logging :as log]
    [integrant.core :as ig])
  (:import
    (com.p6spy.engine.spy P6DataSource)
    (java.io Closeable)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :app.system.service/ref'hikari-data-source-read-write
        :app.system.service/ref'hikari-data-source)
(derive :app.system.service/ref'hikari-data-source-read-only
        :app.system.service/ref'hikari-data-source)

(derive :app.system.service/ref'hikari-data-source
        :app.system.impl/keep-running-on-suspend)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- init-data-source
  [options]
  (log/info "Init Hikari data source" options)
  (let [ds (data-source/create-data-source options)
        spy-wrapped-ds (P6DataSource. ds)]
    spy-wrapped-ds))

(defn- close-data-source!
  [^P6DataSource spy-wrapped-ds]
  (log/info "Close Hikari data source")
  (.close ^Closeable (.unwrap spy-wrapped-ds Closeable)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.service/ref'hikari-data-source
  [k {:keys [dev-mode?] :as options}]
  (e/future
    (init-data-source (-> {:minimum-idle 1
                           :maximum-pool-size 10
                           :connection-timeout 5000
                           :leak-detection-threshold 30000}
                          (cond->
                            dev-mode? (assoc :max-lifetime 300000 :idle-timeout 60000))
                          (merge options)
                          (assoc :read-only? (= k :app.system.service/ref'hikari-data-source-read-only))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :app.system.service/ref'hikari-data-source
  [_ ref'ds]
  (e/future (close-data-source! @ref'ds)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

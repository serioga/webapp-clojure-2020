(ns app.app-system.service.hikari-data-source
  (:require
    [app.lib.database.hikari-data-source :as data-source]
    [app.lib.util.exec :as exec]
    [clojure.tools.logging :as log]
    [integrant.core :as ig])
  (:import
    (com.p6spy.engine.spy P6DataSource)
    (java.io Closeable)))

(set! *warn-on-reflection* true)


(defn ^:private init-data-source
  [options]
  (log/info "Init Hikari data source" options)
  (let [ds (data-source/create-data-source options)
        spy-wrapped-ds (P6DataSource. ds)]
    spy-wrapped-ds))


(defn ^:private close-data-source!
  [^P6DataSource spy-wrapped-ds]
  (log/info "Close Hikari data source")
  (.close ^Closeable (.unwrap spy-wrapped-ds Closeable)))


(derive :app-system.service/ref'hikari-data-source-read-write
        :app-system.service/ref'hikari-data-source)
(derive :app-system.service/ref'hikari-data-source-read-only
        :app-system.service/ref'hikari-data-source)


(defmethod ig/init-key :app-system.service/ref'hikari-data-source
  [k {:keys [dev-mode?] :as options}]
  (exec/future
    (init-data-source (-> {:minimum-idle 1
                           :maximum-pool-size 10
                           :connection-timeout 5000
                           :leak-detection-threshold 30000}
                          (cond->
                            dev-mode? (assoc :max-lifetime 300000 :idle-timeout 60000))
                          (merge options)
                          (assoc :read-only? (= k :app-system.service/ref'hikari-data-source-read-only))))))


(defmethod ig/halt-key! :app-system.service/ref'hikari-data-source
  [_ ref'ds]
  (exec/future (close-data-source! @ref'ds)))


(derive :app-system.service/ref'hikari-data-source
        :app-system.core/keep-running-on-suspend)

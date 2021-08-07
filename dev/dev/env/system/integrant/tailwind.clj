(ns dev.env.system.integrant.tailwind
  (:require [dev.env.reload.ring-refresh :as ring-refresh]
            [dev.env.tailwind.watcher :as tailwind]
            [integrant.core :as ig]
            [mount.core :as mount]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- mount-restart-running
  [state]
  (some-> ((mount/running-states) state)
          (doto (mount/stop)
                (mount/start))))

(defonce ^:private var'handler-was-run (atom false))

(defn- wrap-handler-was-run
  [handler]
  (fn [& args]
    (apply handler args)
    (reset! var'handler-was-run true)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :dev.env.system.integrant/tailwind
  [_ {:keys [webapp watcher dependent-mount-states]}]
  (ig/init-key :dev.env.system.integrant/watcher
               (-> watcher (assoc :handler (-> {:webapp webapp
                                                :on-rebuild (fn []
                                                              (->> dependent-mount-states (run! mount-restart-running))
                                                              (ring-refresh/send-refresh!))}
                                               (tailwind/watch-handler)
                                               (wrap-handler-was-run))
                                  :handler-run-on-init (not @var'handler-was-run)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/halt-key! :dev.env.system.integrant/tailwind
  [_ watcher]
  (ig/halt-key! :dev.env.system.integrant/watcher watcher))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

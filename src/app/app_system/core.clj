(ns app.app-system.core
  (:require
    ; systems
    [app.app-system.service.app-config]
    [app.app-system.service.mount]
    ; imports
    [app.app-system.impl :as impl]
    [app.lib.util.integrant :as ig-util]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


(defonce ^{:doc "Global reference to the running system"}
  app-system (atom nil))


(add-watch app-system :log-system-status
  (fn [_ _ _ system]
    (some-> system impl/log-prop-files)
    (some-> system
      (impl/log-running-rpc-service :app-system.service/*rpc-server))))


(defn system-config
  []
  {:app-system/dev-mode? false

   :app-system.service/mount
   {:app.config.core/app-config (ig/ref :app-system.service/app-config)}


   [:app-system.core/system-property :app-system.options/prop-files]
   {:key "config.file"}

   :app-system.dev/prepare-prop-files nil

   :app-system.service/app-config
   {:prop-files (ig/ref :app-system.options/prop-files)
    :dev/prepare-prop-files (ig/ref :app-system.dev/prepare-prop-files)
    :conform-rules {#"System\.Switch\..+" :edn
                    #".+\.Password" :secret
                    #".+\.Secret" :secret}
    :prop-defaults {}}


   ; Wrap up

   [:app-system.core/init-map :app-system.config/await-before-start]
   {:init-map {:mount (ig/ref :app-system.service/mount)}}})


(defn stop!
  "Stop global system."
  []
  (when-some [system @app-system]
    (reset! app-system nil)
    (ig-util/halt! system)))


(defn suspend!
  "Suspend global system."
  []
  (some-> @app-system (ig-util/suspend!)))


(defn start!
  "Start global system."
  ([]
   (start! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity}}]
   (stop!)
   (let [config (prepare-config (system-config))]
     (reset! app-system
       (ig-util/init config, (or system-keys (keys config)))))))


(defn resume!
  "Resume global system."
  ([]
   (resume! {}))
  ([{:keys [system-keys, prepare-config]
     :or {prepare-config identity} :as options}]
   (if-some [system @app-system]
     (do
       (reset! app-system nil)
       (let [config (prepare-config (system-config))]
         (reset! app-system
           (ig-util/resume config, system, (or system-keys (keys system))))))
     (start! options))))

#_(comment
    (time (keys (start!)))
    (time (suspend!))
    (time (keys (resume!)))
    (time (do
            (suspend!)
            (keys (resume!))))
    (time (stop!))

    (time (:app-system.service/*immutant-web
            (start! {:system-keys [:app-system.service/*immutant-web]}))))


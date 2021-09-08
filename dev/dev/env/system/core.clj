(ns dev.env.system.core
  (:require [clojure.edn :as edn]
            [dev.env.reload.app-reload :as app-reload]
            [dev.env.system.app :as app]
            [lib.clojure.core :as e]
            [lib.clojure.ns :as ns]
            [lib.integrant.core :as ig]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'dev.env.system.integrant._)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private !stats
  (atom {::start-count 0}))

(defn- register-successful-start
  []
  (swap! !stats update ::start-count inc))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- read-config-edn
  [f]
  (try (some-> (slurp f)
               (edn/read-string)
               :dev.env.system/config)
       (catch Throwable e
         (throw (->> e (Exception. (e/prs #'read-config-edn f)))))))

(defn- read-config
  []
  (e/deep-merge (read-config-edn "./dev/dev/config/default.edn")
                (some-> "./dev/dev/config/user.edn"
                        (e/asserted fs/file?)
                        (read-config-edn))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private !system (atom nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop
  "Stop `env` system."
  []
  (swap! !system #(some-> % ig/halt!))
  nil)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start
  "Start `env` system."
  []
  (stop)
  (reset! !system (ig/init (read-config)))
  (register-successful-start)
  nil)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- restart
  "Restart (suspend/resume) `env` system."
  []
  (when-some [system @!system]
    (let [config (read-config)]
      (reset! !system nil)
      (ig/suspend! system)
      (reset! !system (ig/resume config system))))
  nil)

(defn- trigger-watcher
  [k]
  (-> (get @!system k) meta :handler
      (e/assert fn? (str "Trigger watcher " k))
      (e/invoke #'trigger-watcher k)))

(defn- reload
  "Reload actions on ENTER keypress."
  []
  (try
    (app/stop)
    (restart)
    (trigger-watcher :dev.env.system.integrant/app-reload)
    (catch Throwable e
      (app-reload/log-reload-failure e))))

(defn prompt-reload-on-enter
  "Prompts for manual reload on ENTER keypress."
  []
  (app-reload/print-reload-on-enter)
  (while (some? (read-line))
    (reload)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @!system
          :dev.env.system.integrant/nrepl
          e/unwrap-future))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

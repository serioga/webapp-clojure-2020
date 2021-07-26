(ns dev.env.system.core
  (:require [clojure.edn :as edn]
            [dev.env.reload.app-reload :as app-reload]
            [dev.env.system.app :as app]
            [lib.clojure.core :as e]
            [lib.clojure.ns :as ns]
            [lib.integrant.core :as ig]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'dev.env.system.integrant._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private var'stats
  (atom {::start-count 0}))

(defn- register-successful-start!
  []
  (swap! var'stats update ::start-count inc))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- read-config-edn
  [f]
  (e/try-wrap-ex [[#'read-config-edn f]]
    (some-> (slurp f)
            (edn/read-string)
            :dev.env.system/config)))

(defn- read-config
  []
  (e/deep-merge (read-config-edn "./dev/dev/config/default.edn")
                (some-> "./dev/dev/config/user.edn"
                        (e/asserted fs/file?)
                        (read-config-edn))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private var'system (atom nil))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop `env` system."
  []
  (swap! var'system #(some-> % ig/halt!))
  nil)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start!
  "Start `env` system."
  []
  (stop!)
  (reset! var'system (ig/init (read-config)))
  (register-successful-start!)
  nil)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- restart!
  "Restart (suspend/resume) `env` system."
  []
  (when-some [system @var'system]
    (let [config (read-config)]
      (reset! var'system nil)
      (ig/suspend! system)
      (reset! var'system (ig/resume config system))))
  nil)

(defn- trigger-watcher
  [k]
  (-> (get @var'system k) meta :handler
      (e/assert fn? ["Trigger watcher" k])
      (e/invoke #'trigger-watcher k)))

(defn- reload!
  "Reload actions on ENTER keypress."
  []
  (try
    (app/stop!)
    (restart!)
    (trigger-watcher :dev.env.system.integrant/app-reload)
    (catch Throwable ex
      (app-reload/log-reload-failure ex))))

(defn prompt-reload-on-enter
  "Prompts for manual reload on ENTER keypress."
  []
  (app-reload/print-reload-on-enter)
  (while (some? (read-line))
    (reload!)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @var'system
          :dev.env.system.integrant/nrepl
          e/unwrap-future))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns dev.env.system.core
  (:require [clojure.edn :as edn]
            [dev.env.reload.app-reload :as app-reload]
            [dev.env.system.app :as app.system]
            [dev.env.system.integrant :as ig']
            [integrant.core :as ig]
            [lib.clojure.core :as c]
            [lib.clojure.ns :as ns]
            [lib.integrant.system :as ig.system]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'dev.env.system.integrant._)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private stats!
  (atom {::start-count 0}))

(defn- register-successful-start
  []
  (swap! stats! update ::start-count inc))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- read-config-edn
  [f]
  (try (some-> (slurp f)
               (edn/read-string)
               :dev.env.system/config)
       (catch Throwable e
         (throw (->> e (Exception. (c/pr-str* #'read-config-edn f)))))))

(defn- add-repl-dependency
  "Adds dependency for :dev.env.system.integrant/nrepl in every key to start
  REPL first because REPL is required to keep program running even if some key
  fails to start."
  [config]
  (into {} (map (fn [[k v]]
                  [k (cond-> v
                       (and (not= k :dev.env.system.integrant/nrepl)
                            (map? v))
                       (assoc ::nrepl (ig/ref :dev.env.system.integrant/nrepl)))]))
        config))

(defn- read-config
  []
  (-> (c/deep-merge (read-config-edn "./dev/dev/config/default.edn")
                    (some-> (c/select "./dev/dev/config/user.edn" fs/file?)
                            (read-config-edn)))
      (add-repl-dependency)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private system! (atom nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop
  "Stop `env` system."
  []
  (swap! system! #(some-> % ig'/halt!))
  nil)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start
  "Start `env` system."
  []
  (stop)
  (try (reset! system! (ig'/init (read-config)))
       (catch Exception e
         (when-let [system (some-> (ig.system/ex-failed-system e)
                                   (c/select :dev.env.system.integrant/nrepl))]
           ;; Keep partially started system if repl started successfully.
           (reset! system! system))
         (throw e)))
  (register-successful-start)
  nil)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- restart
  "Restart (suspend/resume) `env` system."
  []
  (when-some [system @system!]
    (let [config (read-config)]
      (reset! system! nil)
      (ig'/suspend! system)
      (try (reset! system! (ig'/resume config system))
           (catch Exception e
             (when-let [system (ig.system/ex-failed-system e)]
               (reset! system! system))
             (throw e)))))
  nil)

(defn- trigger-watcher
  [k]
  (-> (get @system! k) meta :handler
      (doto (c/assert-pred fn? (str "Trigger watcher " k)))
      (c/invoke #'trigger-watcher k)))

(defn- reload
  "Reload actions on ENTER keypress."
  []
  (try
    (app.system/stop)
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
  (some-> @system!
          :dev.env.system.integrant/nrepl))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

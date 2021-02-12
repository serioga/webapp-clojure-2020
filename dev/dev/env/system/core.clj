(ns dev.env.system.core
  (:require [app.web-example.impl.html :as example-html]
            [clojure.tools.logging :as log]
            [dev.env.reload.app-reload :as app-reload]
            [dev.env.reload.ring-refresh :as ring-refresh]
            [dev.env.system.app :as app]
            [dev.env.tailwind.watcher :as tailwind]
            [lib.clojure.core :as e]
            [lib.clojure.ns :as ns]
            [lib.integrant.core :as ig]
            [mount.core :as mount]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'dev.env.system.integrant._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- print-reload-on-enter
  []
  (print "\n<< Press ENTER to reload >>\n\n")
  (flush))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- config
  ([] (config {}))
  ([{:keys [first-run?] :or {first-run? true}}]
   {:dev.env.system.integrant/nrepl {:write-port-file ".nrepl-port"}

    :dev.env.system.integrant/shadow-cljs {:builds-to-start [:example]}

    [:dev.env.system.integrant/watcher ::app-reload-watcher]
    {:handler (app-reload/watch-handler {:ns-tracker-dirs ["src" "dev"]
                                         :always-reload-ns ['app.database.core]
                                         :app-stop #'app/suspend!
                                         :app-start #'app/resume!
                                         :on-complete #'print-reload-on-enter})
     :options {:dirs ["src" "dev" "dev-resources/app" "resources/app"]
               ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
               ; :filter will pick out only files that match this pattern.
               :files [".props$" ".clj$" ".cljc$" ".cljs$" ".sql$" ".xml$"]
               ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
               ; :exclude will leave out files that match this pattern.
               :exclude []}}

    [:dev.env.system.integrant/watcher ::tailwind-watcher]
    {:handler (tailwind/watch-handler {:webapp "example"
                                       :on-rebuild (fn [] (when ((mount/running-states) (str #'example-html/styles-css-uri))
                                                            (mount/stop #'example-html/styles-css-uri)
                                                            (mount/start #'example-html/styles-css-uri)
                                                            (ring-refresh/send-refresh!)))})
     :options {:dirs ["tailwind/app/config" "tailwind/app/web_example"]
               :files [".css" ".js$"]}
     :run-handler-on-init? first-run?}}))

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
  (reset! var'system (ig/init (config)))
  nil)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- restart!
  "Restart (suspend/resume) `env` system."
  []
  (when-some [system @var'system]
    (reset! var'system nil)
    (ig/suspend! system)
    (reset! var'system (ig/resume (config {:first-run? false}) system))
    nil))

(defn- reload!
  "Reload actions on ENTER keypress."
  []
  (e/try-log-error "Reload application"
    (app/stop!)
    (restart!)
    (app/start!)
    (log/info "[DONE] Application reload")))

(defn prompt-reload-on-enter
  "Prompts for manual reload on ENTER keypress."
  []
  (print-reload-on-enter)
  (while (some? (read-line))
    (reload!)
    (print-reload-on-enter)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @var'system
          :dev.env.system.integrant/nrepl
          e/unwrap-future))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

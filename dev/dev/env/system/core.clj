(ns dev.env.system.core
  (:require
    [app.web-example.impl.html :as example-html]
    [dev.env.reload.ring-refresh :as ring-refresh]
    [dev.env.system.app :as app]
    [dev.env.tailwind.watcher :as tailwind]
    [lib.clojure.ns :as ns]
    [lib.integrant.core :as ig]
    [mount.core :as mount]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns/require-dir 'dev.env.system.integrant._)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private var'system (atom nil))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- config []
  {:dev.env.system/ref'nrepl {:write-port-file ".nrepl-port"}

   :dev.env.system/app-reload {:ns-tracker-dirs ["src" "dev"]
                               :app-start #'app/start!
                               :app-suspend #'app/suspend!
                               :app-resume #'app/resume!
                               :app-stop #'app/stop!
                               :always-reload-ns ['app.database.core]}

   [:dev.env.system/watcher :dev.env.system/app-reload-watcher]
   {:handler (ig/ref :dev.env.system/app-reload)
    :options {:dirs ["src" "dev" "dev-resources/app" "resources/app"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :filter will pick out only files that match this pattern.
              :files [".props$" ".clj$" ".cljc$" ".cljs$" ".sql$" ".xml$"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :exclude will leave out files that match this pattern.
              :exclude []}}

   :dev.env.system/ref'shadow-cljs {:builds-to-start [:example]}

   [:dev.env.system/watcher :dev.env.system/tailwind-watcher]
   {:handler (tailwind/watcher-handler {:webapp "example"
                                        :on-rebuild (fn []
                                                      (when ((mount/running-states) (str #'example-html/styles-css-uri))
                                                        (mount/stop #'example-html/styles-css-uri)
                                                        (mount/start #'example-html/styles-css-uri)
                                                        (ring-refresh/send-refresh!)))})
    :options {:dirs ["tailwind/app/config" "tailwind/app/web_example"]
              :files [".css" ".js$"]}
    :run-handler-on-init? true}})

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop `env` system."
  []
  (swap! var'system #(some-> % ig/halt!)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start!
  "Start `env` system."
  ([] (start! (config) nil))
  ([config] (start! config nil))
  ([config, init-keys]
   (stop!)
   (reset! var'system (ig/init config, (or init-keys (keys config))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn reload-on-enter
  "Reload actions on ENTER keypress."
  []
  (when-some [reload (some-> @var'system
                             :dev.env.system/app-reload
                             meta :reload-on-enter)]
    (reload)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @var'system
          :dev.env.system/ref'nrepl
          deref))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

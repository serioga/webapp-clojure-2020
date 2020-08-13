(ns dev.env.system.core
  (:require                                                 ; systems
    [dev.env.system.unit.app-reload]
    [dev.env.system.unit.nrepl]
    [dev.env.system.unit.ring-refresh :as ring-refresh]
    [dev.env.system.unit.shadow-cljs]
    [dev.env.system.unit.tailwind :as tailwind]
    [dev.env.system.unit.watcher])
  (:require
    [app.lib.util.integrant :as ig-util]
    [app.web-example.impl.html-page :as html-page]
    [dev.env.system.app :as app]
    [integrant.core :as ig]
    [mount.core :as mount]))

(set! *warn-on-reflection* true)


(defonce ^:private var'system (atom nil))


(defn- config []
  {:dev.env.system/ref'nrepl {:write-port-file ".nrepl-port"}

   :dev.env.system/app-reload {:ns-tracker-dirs ["src" "dev"]
                               :app-start #'app/start!
                               :app-suspend #'app/suspend!
                               :app-resume #'app/resume!
                               :app-stop #'app/stop!
                               :always-reload-ns ['app.database.core]}

   [:dev.env.system/ref'watcher :dev.env.system/ref'app-reload-watcher]
   {:handler (ig/ref :dev.env.system/app-reload)
    :options {:dirs ["src" "dev" "dev-resources/app" "resources/app"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :filter will pick out only files that match this pattern.
              :files [".props$" ".clj$" ".cljc$" ".cljs$" ".sql$"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :exclude will leave out files that match this pattern.
              :exclude []}}

   :dev.env.system/ref'shadow-cljs {:builds-to-start [:example]}

   [:dev.env.system/ref'watcher :dev.env.system/ref'tailwind]
   {:handler (tailwind/watcher-handler {:webapp "example"
                                        :on-rebuild (fn []
                                                      (mount/stop #'html-page/styles-css-uri)
                                                      (mount/start #'html-page/styles-css-uri)
                                                      (ring-refresh/send-refresh!))})
    :options {:dirs ["tailwind/app/config" "tailwind/app/web_example"]
              :files [".css" ".js$"]}
    :run-handler-on-init? true}})


(defn stop!
  "Stop `env` system."
  []
  (swap! var'system #(some-> % ig-util/halt!)))


(defn start!
  "Start `env` system."
  ([]
   (start! (config) nil))
  ([config]
   (start! config nil))
  ([config, init-keys]
   (stop!)
   (reset! var'system (ig-util/init config, (or init-keys (keys config))))))


(defn reload-on-enter
  "Reload actions on ENTER keypress."
  []
  (when-some [reload (some-> @var'system
                             :dev.env.system/app-reload
                             meta :reload-on-enter)]
    (reload)))


(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @var'system
          :dev.env.system/ref'nrepl
          deref))


(comment
  (time (let [system (start!)]
          (keys system)))

  (time (stop!))

  (time (do
          (start!)
          (stop!))))

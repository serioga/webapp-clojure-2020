(ns dev.app.system.core
  (:require                                                 ; systems
    [dev.app.system.unit.app-reload]
    [dev.app.system.unit.nrepl]
    [dev.app.system.unit.ring-refresh :as ring-refresh]
    [dev.app.system.unit.shadow-cljs]
    [dev.app.system.unit.tailwind :as tailwind]
    [dev.app.system.unit.watcher])
  (:require
    [app.lib.util.integrant :as ig-util]
    [app.web-example.impl.html-page :as html-page]
    [dev.app.system.wrap :as app]
    [integrant.core :as ig]
    [mount.core :as mount]))

(set! *warn-on-reflection* true)


(defonce ^:private var'dev-system (atom nil))


(defn- config []
  {:dev-system/ref'nrepl {:write-port-file ".nrepl-port"}

   :dev-system/app-reload {:ns-tracker-dirs ["src" "dev"]
                           :app-start #'app/start!
                           :app-suspend #'app/suspend!
                           :app-resume #'app/resume!
                           :app-stop #'app/stop!
                           :always-reload-ns ['app.database.core]}

   [:dev-system/ref'watcher :dev-system/ref'app-reload-watcher]
   {:handler (ig/ref :dev-system/app-reload)
    :options {:dirs ["src" "dev" "dev-resources/dev" "resources/app"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :filter will pick out only files that match this pattern.
              :files [".props$" ".clj$" ".cljc$" ".cljs$" ".sql$"]

              ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
              ; :exclude will leave out files that match this pattern.
              :exclude []}}

   :dev-system/ref'shadow-cljs {:builds-to-start [:example]}

   [:dev-system/ref'watcher :dev-system/ref'tailwind]
   {:handler (tailwind/watcher-handler {:webapp "example"
                                        :on-rebuild (fn []
                                                      (mount/stop #'html-page/styles-css-uri)
                                                      (mount/start #'html-page/styles-css-uri)
                                                      (ring-refresh/send-refresh!))})
    :options {:dirs ["tailwind/app/config" "tailwind/app/web_example"]
              :files [".css" ".js$"]}
    :run-handler-on-init? true}})


(defn stop!
  "Stop global system."
  []
  (swap! var'dev-system #(some-> % ig-util/halt!)))


(defn start!
  "Start global system."
  ([]
   (start! (config) nil))
  ([config]
   (start! config nil))
  ([config, init-keys]
   (stop!)
   (reset! var'dev-system (ig-util/init config, (or init-keys (keys config))))))


(defn reload-on-enter
  "Reload actions on ENTER keypress."
  []
  (when-some [reload (some-> @var'dev-system
                             :dev-system/app-reload
                             meta :reload-on-enter)]
    (reload)))


(defn nrepl-server
  "Get reference to global nREPL server instance."
  []
  (some-> @var'dev-system
          :dev-system/ref'nrepl
          deref))


(comment
  (time (let [system (start!)]
          (keys system)))

  (time (stop!))

  (time (do
          (start!)
          (stop!))))

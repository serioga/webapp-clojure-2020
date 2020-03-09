(ns dev.dev-system.core
  (:require
    ; systems
    [dev.dev-system.unit.app-reload]
    [dev.dev-system.unit.nrepl]
    [dev.dev-system.unit.shadow-cljs]
    [dev.dev-system.unit.ring-refresh :as ring-refresh]
    [dev.dev-system.unit.tailwind :as tailwind]
    [dev.dev-system.unit.watcher]
    ; imports
    [app.lib.util.integrant :as ig-util]
    [dev.dev-system.app-system :as app-system]
    [integrant.core :as ig]
    [mount.core :as mount]))

(set! *warn-on-reflection* true)


(defonce var'dev-system (atom nil))


(defn config []
  {:dev-system/ref'nrepl {:write-port-file ".nrepl-port"}

   :dev-system/app-reload {:ns-tracker-dirs ["src" "dev"]
                           :app-start #'app-system/start!
                           :app-suspend #'app-system/suspend!
                           :app-resume #'app-system/resume!
                           :app-stop #'app-system/stop!
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
   {:handler (tailwind/watcher-handler
               {:webapp "example"
                :on-rebuild (fn []
                              (mount/stop #'app.web-example.impl.html-page/styles-css-uri)
                              (mount/start #'app.web-example.impl.html-page/styles-css-uri)
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
  []
  (when-some [reload (some-> @var'dev-system
                             :dev-system/app-reload
                             meta :reload-on-enter)]
    (reload)))


(defn nrepl-server
  []
  (some-> @var'dev-system
          :dev-system/ref'nrepl
          deref))


#_(comment
    (time (let [system (start!)]
            (keys system)))

    (time (stop!))

    (time (do
            (start!)
            (stop!))))

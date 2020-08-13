(ns dev.env.system.unit.shadow-cljs
  (:require
    [app.lib.util.exec :as e]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [shadow.cljs.devtools.api :as shadow]
    [shadow.cljs.devtools.server :as server]))

(set! *warn-on-reflection* true)


(defn- start-shadow!
  [{:keys [builds-to-start] :as options}]
  (log/info "Start Shadow CLJS" options)
  (server/start!)
  (doseq [build builds-to-start]
    (shadow/watch build)))


(defn- stop-shadow!
  [system]
  (log/info "Stop Shadow CLJS" system)
  (server/stop!))


(defmethod ig/init-key :dev-system/ref'shadow-cljs
  [_ options]
  (e/future (start-shadow! options)))


(defmethod ig/halt-key! :dev-system/ref'shadow-cljs
  [_ ref'system]
  (e/future (stop-shadow! @ref'system)))


(comment
  (server/start!)
  (server/stop!)
  (shadow/compile :example)
  (shadow/repl :example)
  (shadow/watch :example)
  (shadow/watch-compile-all!))


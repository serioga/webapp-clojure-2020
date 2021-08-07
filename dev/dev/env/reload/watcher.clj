(ns dev.env.reload.watcher
  (:require [clojure.tools.logging :as log]
            [hara.io.watch :as watch]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- locking-handler
  "Wraps handler with locking for execution period."
  [handler]
  (let [var'running (atom false)]
    (fn [& args]
      (when (compare-and-set! var'running false true)
        (try
          (log/debug "Trigger watcher" (str handler) args)
          (apply handler args)
          (finally
            (reset! var'running false)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start-watcher
  "Starts the watcher."
  [handler, {:keys [dirs files exclude] :as options}]
  (log/info "Start watcher" options)
  (let [handler (locking-handler handler)]
    (-> (watch/start-watcher (watch/watcher dirs handler {#_#_:types #{:modify}
                                                          :filter files
                                                          :exclude exclude
                                                          :mode :async}))
        (vary-meta assoc :handler handler))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop-watcher
  "Stops the watcher."
  [watcher]
  (log/info "Stop watcher" watcher)
  (watch/stop-watcher watcher))

(comment
  (time (let [w (time (start-watcher (fn [& reason] (println reason))

                                     {:dirs ["src" "resources/app" "dev"]

                                      ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
                                      ; :filter will pick out only files that match this pattern.
                                      :files [".props$" ".clj$" ".cljc$" ".js$" ".xml$"
                                              ".sql$" ".properties$" ".mustache$" ".yaml"]

                                      ; http://docs.caudate.me/hara/hara-io-watch.html#watch-options
                                      ; :exclude will leave out files that match this pattern.
                                      :exclude []}))]

          (time (stop-watcher w)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns dev.env.reload.watcher
  (:require [hara.io.watch :as watch]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- locking-handler
  "Wraps handler with locking for execution period."
  [handler]
  (let [!running (atom false)]
    (fn [& args]
      (when (compare-and-set! !running false true)
        (try
          (logger/debug logger (e/pr-str* "Trigger watcher" (str handler) args))
          (apply handler args)
          (finally
            (reset! !running false)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start-watcher
  "Starts the watcher."
  [handler, {:keys [dirs files exclude] :as options}]
  (logger/info logger (e/pr-str* "Start watcher" options))
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
  (logger/info logger (e/pr-str* "Stop watcher" watcher))
  (watch/stop-watcher watcher))

(comment
  (time (let [w (time (start-watcher (fn [& reason] (println reason))

                                     {:dirs ["src" "resources/app" "dev"]

                                      ;; See http://docs.caudate.me/hara/hara-io-watch.html#watch-options
                                      ;; :filter will pick out only files that match this pattern.
                                      :files [".props$" ".clj$" ".cljc$" ".js$" ".xml$"
                                              ".sql$" ".properties$" ".mustache$" ".yaml"]

                                      ;; See http://docs.caudate.me/hara/hara-io-watch.html#watch-options
                                      ;; :exclude will leave out files that match this pattern.
                                      :exclude []}))]

          (time (stop-watcher w)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

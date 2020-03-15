(ns dev.dev-system.unit.app-reload
  (:require
    [app.lib.util.exec :as e]
    [clojure.main :as main]
    [clojure.set :as set]
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [ns-tracker.core :as ns-tracker]))

(set! *warn-on-reflection* true)


(defn- reload-on-enter
  [handler]
  (print "\n\n***** Press ENTER to reload application *****\n\n\n")
  (flush)
  (when (some? (read-line))
    (handler :force-reload "ENTER key pressed...")))


(defn- exception-log-msg
  [ex]
  (-> ex
      (Throwable->map)
      (main/ex-triage)
      (main/ex-str)))


(defn- reload-modified-namespaces
  "Return vector of reload errors."
  [ns-tracker always-reload-ns]
  (let [modified (ns-tracker)
        reload-always (into '() (set/difference (set always-reload-ns)
                                                (set modified)))
        var'reload-errors (volatile! [])
        reload-ns (fn [ns-sym]
                    (try
                      (require ns-sym :reload)
                      (log/info "[OK]" "Reload" ns-sym)
                      (catch Throwable ex
                        (let [msg (exception-log-msg ex)]
                          (vswap! var'reload-errors conj [ns-sym msg])
                          (log/info "[FAIL]" "Reload" ns-sym)))))]
    (when-let [namespaces (seq (concat modified reload-always))]
      (log/info "Reloading namespaces:" (string/join ", " namespaces))
      (run! reload-ns namespaces))
    @var'reload-errors))


(defn- watcher-handler
  [{:keys [ns-tracker-dirs, always-reload-ns,
           app-start, app-suspend, app-resume, app-stop]
    :or {app-stop (constantly nil)
         app-suspend (constantly nil)
         app-resume (constantly nil)
         app-start (constantly nil)}}]
  (let [var'reloading? (atom false)
        ns-tracker (ns-tracker/ns-tracker ns-tracker-dirs)
        app-reload
        (fn app-reload
          [& reason]
          (let [[start stop] (if (= :force-reload (first reason))
                               [app-start app-stop]
                               [app-resume app-suspend])]
            (when (compare-and-set! var'reloading? false true)
              (Thread/sleep 200)                            ; pause just in case if several files were updated
              (log/info reason)
              (log/info "[START]" "Application reload")
              (e/try-log-error "Stop application before namespace reloading"
                (stop))
              (if-some [reload-errors (seq (reload-modified-namespaces ns-tracker always-reload-ns))]
                (do
                  (log/info "[FAIL]" "Application reload")
                  (doseq [[ns err] reload-errors]
                    (log/error "[FAIL]" "Reload" ns (str "\n\n" err "\n"))))
                (try
                  (start)
                  (log/info "[DONE]" "Application reload")
                  (catch Throwable ex
                    (log/error (e/ex-message-all ex))
                    (log/info "[FAIL]" "Application reload"))))
              (reset! var'reloading? false)
              (reload-on-enter app-reload))))]
    (-> app-reload
        (with-meta {:reload-on-enter (fn [] (reload-on-enter app-reload))}))))


(defmethod ig/init-key :dev-system/app-reload
  [_ options]
  (watcher-handler options))

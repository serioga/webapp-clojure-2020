(ns dev.env.reload.app-reload
  (:require [clojure.main :as main]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lib.clojure.core :as e]
            [ns-tracker.core :as ns-tracker])
  (:import (java.io FileNotFoundException)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- exception-log-msg
  [ex]
  (-> ex
      (Throwable->map)
      (main/ex-triage)
      (main/ex-str)))

(defn- ns-unalias-all
  "Removes all aliases in namespace."
  [ns-sym]
  (doseq [[alias-sym _] (e/try-ignore (ns-aliases ns-sym))]
    (ns-unalias ns-sym alias-sym)))

(defn- reload-modified-namespaces
  "Return vector of reload errors."
  [ns-tracker always-reload-ns]
  (let [modified (ns-tracker)
        reload-always (into '() (set/difference (set always-reload-ns)
                                                (set modified)))
        var'reload-errors (volatile! [])
        reload-ns (fn [ns-sym]
                    (try
                      (ns-unalias-all ns-sym)
                      (require ns-sym :reload)
                      (log/info "[OK]" "Reload" ns-sym)
                      (catch FileNotFoundException _
                        (remove-ns ns-sym))
                      (catch Throwable ex
                        (if (e/try-ignore (require ns-sym :reload-all)
                                          true)
                          (log/info "[OK]" "Reload" ns-sym)
                          (let [msg (exception-log-msg ex)]
                            (vswap! var'reload-errors conj [ns-sym msg])
                            (log/info "[FAIL]" "Reload" ns-sym))))))]
    (when-let [namespaces (seq (concat modified reload-always))]
      (log/info "Reloading namespaces:" (string/join ", " namespaces))
      (run! reload-ns namespaces))
    @var'reload-errors))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn watcher-handler
  "Builds app reloading function to be used in file watcher."
  [{:keys [ns-tracker-dirs, always-reload-ns, app-stop, app-start, on-complete]}]
  (let [ns-tracker (ns-tracker/ns-tracker ns-tracker-dirs)]
    (fn app-reload [& reason]
      (log/info reason)
      (log/info "[START]" "Application reload")
      (when app-stop
        (e/try-log-error ["Stop application before namespace reloading"]
          (app-stop)))
      (if-some [reload-errors (seq (reload-modified-namespaces ns-tracker always-reload-ns))]
        (do
          (log/info "[FAIL]" "Application reload")
          (doseq [[ns err] reload-errors]
            (log/error "[FAIL]" "Reload" ns (str "\n\n" err "\n"))))
        (try
          (when app-start (app-start))
          (log/info "[DONE]" "Application reload")
          (catch Throwable ex
            (log/error (e/ex-message-all ex))
            (log/info "[FAIL]" "Application reload"))))
      (when on-complete (on-complete)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

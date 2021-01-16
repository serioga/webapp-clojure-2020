(ns dev.env.reload.app
  (:require [clojure.main :as main]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lib.clojure.core :as e]
            [ns-tracker.core :as ns-tracker])
  (:import (java.io FileNotFoundException)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- reload-on-enter
  [handler]
  (future
    (print "\n\nPress ENTER to reload application: ")
    (flush)
    (when (some? (read-line))
      (handler :force-reload "ENTER key pressed..."))))

(defn- with-meta-reload-on-enter
  [f]
  (with-meta f {:reload-on-enter (partial reload-on-enter f)}))

(defn- exception-log-msg
  [ex]
  (-> ex
      (Throwable->map)
      (main/ex-triage)
      (main/ex-str)))

(defn- ns-alias-clash
  "Extracts data from exception message about clashed ns alias."
  [msg]
  (when-some [[_ alias-sym, ns, alias-ns]
              (re-matches #"Alias (\S+) already exists in namespace (\S+), aliasing (\S+)" msg)]
    {:ns (symbol ns)
     :alias-sym (symbol alias-sym)
     :alias-ns (symbol alias-ns)}))

(defn- reload-autofix
  "Tries to reload namespace with automatic fixing of:
   - alias already exists."
  ([ns-sym] (reload-autofix ns-sym #{}))
  ([ns-sym aliases]
   (try
     (require ns-sym :reload)
     (catch Throwable ex
       (let [{:keys [alias-sym]} (ns-alias-clash (-> ex e/ex-root-cause ex-message))
             try-fix-alias? (and alias-sym (not (aliases alias-sym)))]
         (cond
           try-fix-alias? (do (ns-unalias ns-sym alias-sym)
                              (reload-autofix ns-sym (conj aliases alias-sym)))
           :else (throw ex)))))))

(defn- reload-modified-namespaces
  "Return vector of reload errors."
  [ns-tracker always-reload-ns]
  (let [modified (ns-tracker)
        reload-always (into '() (set/difference (set always-reload-ns)
                                                (set modified)))
        var'reload-errors (volatile! [])
        reload-ns (fn [ns-sym]
                    (try
                      (reload-autofix ns-sym)
                      (log/info "[OK]" "Reload" ns-sym)
                      (catch FileNotFoundException _
                        (remove-ns ns-sym))
                      (catch Throwable ex
                        (let [msg (exception-log-msg ex)]
                          (vswap! var'reload-errors conj [ns-sym msg])
                          (log/info "[FAIL]" "Reload" ns-sym)))))]
    (when-let [namespaces (seq (concat modified reload-always))]
      (log/info "Reloading namespaces:" (string/join ", " namespaces))
      (run! reload-ns namespaces))
    @var'reload-errors))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn watcher-handler
  "Builds app reloading function to be used in file watcher."
  [{:keys [ns-tracker-dirs, always-reload-ns,
           app-start, app-suspend, app-resume, app-stop]}]
  (let [ns-tracker (ns-tracker/ns-tracker ns-tracker-dirs)]

    (-> (fn app-reload [& reason]
          (let [[start stop] (if (= :force-reload (first reason))
                               [app-start app-stop]
                               [app-resume app-suspend])]
            (log/info reason)
            (log/info "[START]" "Application reload")
            (when stop
              (e/try-log-error ["Stop application before namespace reloading"]
                (stop)))
            (if-some [reload-errors (seq (reload-modified-namespaces ns-tracker always-reload-ns))]
              (do
                (log/info "[FAIL]" "Application reload")
                (doseq [[ns err] reload-errors]
                  (log/error "[FAIL]" "Reload" ns (str "\n\n" err "\n"))))
              (try
                (when start (start))
                (log/info "[DONE]" "Application reload")
                (catch Throwable ex
                  (log/error (e/ex-message-all ex))
                  (log/info "[FAIL]" "Application reload"))))
            (reload-on-enter app-reload)))

        (with-meta-reload-on-enter))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

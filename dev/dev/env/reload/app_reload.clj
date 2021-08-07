(ns dev.env.reload.app-reload
  (:require [clojure.main :as main]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lib.clojure.core :as e]
            [ns-tracker.core :as ns-tracker])
  (:import (java.io FileNotFoundException)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- ns-unalias-all
  "Removes all aliases in namespace."
  [ns-sym]
  (doseq [[alias-sym _] (e/try-ignore (ns-aliases ns-sym))]
    (ns-unalias ns-sym alias-sym)))

(defn- reload-ns
  "Reloads ns and returns nil or `[ns-sym err-msg]`."
  [ns-sym]
  (try
    (ns-unalias-all ns-sym)
    (require ns-sym :reload)
    (log/info "[OK]" "Reload" ns-sym)
    nil
    (catch FileNotFoundException _
      (remove-ns ns-sym)
      nil)
    (catch Throwable ex
      [ns-sym ex])))

(defn- reload-namespaces
  "Returns vector of reload errors."
  [namespaces]
  (when-let [namespaces (some->> namespaces seq distinct)]
    ;; Reload can fail due to the incorrect order of namespaces.
    ;; So we reload multiple times recursively while this reduces amount of failed namespaces.
    (loop [xs (mapv vector namespaces)]
      (log/info "Reloading namespaces:" (string/join ", " (map first xs)))
      (let [errors (->> xs (into [] (comp (map first) (keep reload-ns))))]
        (if (and (seq errors), (< (count errors) (count xs)))
          (recur errors)
          errors)))))

(defn- log-reload-error
  [[ns ex]]
  (log/error "[FAIL]" "Reload" (str ns "\n\n" (-> ex Throwable->map main/ex-triage main/ex-str))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private ^{:doc "Keeps namespace reload errors."}
  !reload-errors (atom nil))

(defn watch-handler
  "Builds app reloading function to be used in file watcher."
  [{:keys [ns-tracker-dirs, always-reload-ns, never-reload-ns,
           app-stop, app-start, on-success, on-failure]}]
  (let [ns-tracker (ns-tracker/ns-tracker ns-tracker-dirs)]
    (fn app-reload [& _]
      (when app-stop
        (e/try-log-error ["Stop application before namespace reloading"]
          (app-stop)))
      (if-some [errors (seq (->> (concat always-reload-ns (ns-tracker) (map first @!reload-errors))
                                 (remove (set never-reload-ns))
                                 (reload-namespaces)
                                 (reset! !reload-errors)))]
        (do
          (run! log-reload-error errors)
          (when on-failure
            (on-failure (e/ex-info ["Failed to reload namespaces" (map first errors)]
                                   {:reason ::reload-namespaces, :errors errors}))))
        (try
          (when app-start (app-start))
          (when on-success (on-success))
          (catch Throwable ex
            (when on-failure (on-failure ex))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn print-reload-on-enter
  "Prints prompt for application reload."
  []
  (print "\n<< Press ENTER to reload >>\n\n")
  (flush))

(defn log-reload-success
  "Prints confirmation of the successful application reload."
  []
  (log/info "[DONE] Application reload")
  (print-reload-on-enter))

(defn log-reload-failure
  "Prints error if application reload failed."
  [ex]
  (log/error (e/ex-message-all ex))
  (log/info "[FAIL] Application reload")
  (print-reload-on-enter))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

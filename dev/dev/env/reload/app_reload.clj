(ns dev.env.reload.app-reload
  (:require [clojure.main :as main]
            [clojure.string :as string]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]
            [ns-tracker.core :as ns-tracker])
  (:import (java.io FileNotFoundException)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(defn- ns-unalias-all
  "Removes all aliases in namespace."
  [ns-sym]
  (doseq [[alias-sym _] (try (ns-aliases ns-sym) (catch Throwable _))]
    (ns-unalias ns-sym alias-sym)))

(defn- reload-ns
  "Reloads ns and returns nil or `[ns-sym err-msg]`."
  [ns-sym]
  (try
    (ns-unalias-all ns-sym)
    (require ns-sym :reload)
    (logger/info logger (str "[OK] Reload " ns-sym))
    nil
    (catch FileNotFoundException _
      (remove-ns ns-sym)
      nil)
    (catch Throwable e
      [ns-sym e])))

(defn- reload-namespaces
  "Returns vector of reload errors."
  [namespaces]
  (when-let [namespaces (some->> namespaces seq distinct)]
    ;; Reload can fail due to the incorrect order of namespaces.
    ;; So we reload multiple times recursively while this reduces amount of failed namespaces.
    (loop [xs (mapv vector namespaces)]
      (logger/info logger (str "Reloading namespaces: " (string/join ", " (map first xs))))
      (let [errors (->> xs (into [] (comp (map first) (keep reload-ns))))]
        (if (and (seq errors), (< (count errors) (count xs)))
          (recur errors)
          errors)))))

(defn- log-reload-error
  [[failed-ns, throwable]]
  (logger/error logger (str "[FAIL] Reload " (str failed-ns "\n\n" (-> throwable Throwable->map main/ex-triage main/ex-str)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defonce ^:private ^{:doc "Keeps namespace reload errors."}
  reload-errors! (atom nil))

(defn watch-handler
  "Builds app reloading function to be used in file watcher."
  [{:keys [ns-tracker-dirs, always-reload-ns, never-reload-ns, never-reload-ns-in,
           app-stop, app-start, on-success, on-failure]}]
  (let [ns-tracker (ns-tracker/ns-tracker ns-tracker-dirs)]
    (fn app-reload [& _]
      (when app-stop
        (try (app-stop) (catch Throwable e
                          (logger/log-throwable logger e "Stop application before namespace reloading"))))
      (if-some [errors (seq (->> (concat always-reload-ns (ns-tracker) (map first @reload-errors!))
                                 (remove (set never-reload-ns))
                                 (remove (fn [n] (->> never-reload-ns-in (some (partial string/starts-with? (str n "."))))))
                                 (reload-namespaces)
                                 (reset! reload-errors!)))]
        (do
          (run! log-reload-error errors)
          (when on-failure
            (on-failure (ex-info (c/pr-str* "Failed to reload namespaces" (map first errors))
                                 {:reason ::reload-namespaces, :errors errors}))))
        (try
          (when app-start (app-start))
          (when on-success (on-success))
          (catch Throwable e
            (when on-failure (on-failure e))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn print-reload-on-enter
  "Prints prompt for application reload."
  []
  (print "\n<< Press ENTER to reload >>\n\n")
  (flush))

(defn log-reload-success
  "Prints confirmation of the successful application reload."
  []
  (logger/info logger "[DONE] Application reload")
  (print-reload-on-enter))

(defn log-reload-failure
  "Prints error if application reload failed."
  [throwable]
  (logger/error logger (c/ex-message-all throwable))
  (logger/info logger "[FAIL] Application reload")
  (print-reload-on-enter))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

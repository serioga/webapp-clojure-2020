(ns dev.dev-system.unit.ring-refresh
  (:require
    [clojure.tools.logging :as log]
    [compojure.core :as compojure]
    [ring.middleware.params :as params]
    [ring.middleware.refresh :as refresh])
  (:import
    (java.util UUID Date)))

(set! *warn-on-reflection* true)


(def ^:private var'refresh-state
  (atom {:last-modified (Date.)
         :reload? false}))


(defn ^:private watch-until
  [reference, pred, timeout-ms]
  (let [result (promise)
        watch-key (str (UUID/randomUUID))]
    (try
      (add-watch reference watch-key
        (fn [_ _ _ value]
          (deliver result (pred value))))
      (if-some [v (pred @reference)]
        v
        (deref result timeout-ms false))
      (finally
        (remove-watch reference watch-key)))))


(def ^:private source-changed-route
  (compojure/GET "/__source_changed" [since]
    (let [timestamp (Long/parseLong since)]
      (str (watch-until var'refresh-state
             (fn [{:keys [last-modified, reload?]}]
               (when (> (.getTime last-modified) timestamp)
                 reload?))
             60000)))))


(defn wrap-refresh
  "Modified `ring.middleware.refresh/wrap-refresh`."
  [handler]
  (params/wrap-params
    (compojure/routes
      source-changed-route
      (@#'refresh/wrap-with-script handler @#'refresh/refresh-script))))


(defn send-refresh!
  "Send response to pages with flag if they should
   reload page or just reconnect to __source_changed."
  ([] (send-refresh! (:reload? @var'refresh-state)))
  ([reload?]
   (when reload?
     (log/info "Send refresh command to browser pages"))
   (reset! var'refresh-state {:last-modified (Date.)
                              :reload? reload?})))


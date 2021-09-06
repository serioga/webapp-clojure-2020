(ns dev.env.reload.ring-refresh
  (:require [compojure.core :as compojure]
            [lib.clojure-tools-logging.logger :as logger]
            [ring.middleware.params :as params]
            [ring.middleware.refresh :as refresh])
  (:import (java.util UUID Date)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private !refresh-state
  (atom {::last-modified (Date.)
         ::refresh-is-enabled false}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- watch-until
  [?state, pred, timeout-ms]
  (let [?result (promise)
        watch-key (str (UUID/randomUUID))]
    (try
      (add-watch ?state watch-key (fn [_ _ _ value]
                                    (deliver ?result (pred value))))
      (if-some [v (pred @?state)] v
                                  (deref ?result timeout-ms false))
      (finally
        (remove-watch ?state watch-key)))))

(def ^:private source-changed-route
  (compojure/GET "/__source_changed" [since]
    (let [timestamp (Long/parseLong since)]
      (str (watch-until !refresh-state (fn [{::keys [last-modified, refresh-is-enabled]}]
                                         (when (> (.getTime ^Date last-modified) timestamp)
                                           refresh-is-enabled))
                        60000)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-refresh
  "Modified `ring.middleware.refresh/wrap-refresh`."
  [handler]
  (params/wrap-params (compojure/routes source-changed-route
                                        (@#'refresh/wrap-with-script handler @#'refresh/refresh-script))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn send-refresh
  "Send response to pages with flag if they should
   reload page or just reconnect to __source_changed."
  ([] (send-refresh (::refresh-is-enabled @!refresh-state)))
  ([refresh-is-enabled]
   (when refresh-is-enabled
     (logger/info (logger/get-logger *ns*) "Send refresh command to browser pages"))
   (reset! !refresh-state {::last-modified (Date.)
                           ::refresh-is-enabled refresh-is-enabled})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

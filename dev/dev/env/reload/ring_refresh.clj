(ns dev.env.reload.ring-refresh
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [ring.middleware.params :as params]
            [ring.middleware.refresh :as refresh])
  (:import (java.util UUID Date)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private var'refresh-state
  (atom {::last-modified (Date.)
         ::refresh-is-enabled false}))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- watch-until
  [ref'state, pred, timeout-ms]
  (let [ref'result (promise)
        watch-key (str (UUID/randomUUID))]
    (try
      (add-watch ref'state watch-key (fn [_ _ _ value]
                                       (deliver ref'result (pred value))))
      (if-some [v (pred @ref'state)] v
                                     (deref ref'result timeout-ms false))
      (finally
        (remove-watch ref'state watch-key)))))

(def ^:private source-changed-route
  (compojure/GET "/__source_changed" [since]
    (let [timestamp (Long/parseLong since)]
      (str (watch-until var'refresh-state (fn [{::keys [last-modified, refresh-is-enabled]}]
                                            (when (> (.getTime ^Date last-modified) timestamp)
                                              refresh-is-enabled))
                        60000)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-refresh
  "Modified `ring.middleware.refresh/wrap-refresh`."
  [handler]
  (params/wrap-params (compojure/routes source-changed-route
                                        (@#'refresh/wrap-with-script handler @#'refresh/refresh-script))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn send-refresh!
  "Send response to pages with flag if they should
   reload page or just reconnect to __source_changed."
  ([] (send-refresh! (::refresh-is-enabled @var'refresh-state)))
  ([refresh-is-enabled]
   (when refresh-is-enabled
     (log/info "Send refresh command to browser pages"))
   (reset! var'refresh-state {::last-modified (Date.)
                              ::refresh-is-enabled refresh-is-enabled})))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

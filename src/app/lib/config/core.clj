(ns app.lib.config.core
  (:require
    [app.lib.util.exec :as exec]))

(set! *warn-on-reflection* true)


(defn get-required
  [config key]
  (if (contains? config key)
    (config key)
    (exec/throw-ex-info "Missing configuration property" (pr-str key))))


(defn get-optional
  ([config key]
   (get-optional config key nil))
  ([config key default]
   (get config key default)))

(ns app.lib.config.core
  (:require
    [app.lib.util.exec :as e]))

(set! *warn-on-reflection* true)


(defn get-required
  "Get required value from `config` map.
   Raise exception for missing keys."
  [config key]
  (if (contains? config key)
    (config key)
    (e/throw-ex-info "Missing configuration property" (pr-str key))))


(defn get-optional
  "Get optional value from `config` map.
   Return `nil` or `default` for missing keys."
  ([config key]
   (get-optional config key nil))
  ([config key default]
   (get config key default)))

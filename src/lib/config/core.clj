(ns lib.config.core
  (:require [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-required
  "Get required value from config.
   Raise exception for missing keys."
  [config key]
  (let [nan (Object.), val (config key nan)]
    (when (identical? val nan)
      (throw (e/ex-info ["Missing configuration property" key])))
    val))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-optional
  "Get optional value from config.
   Return `nil` or `default` for missing keys."
  ([config key]
   (get-optional config key nil))
  ([config key default]
   (get config key default)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

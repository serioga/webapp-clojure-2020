(ns lib.config.core)

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-required
  "Get required value from config.
   Raise exception for missing keys."
  [config k]
  (let [none (Object.), v (config k none)]
    (when (identical? v none)
      (throw (Exception. (str "Missing configuration property " k))))
    v))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-optional
  "Get optional value from config.
   Return `nil` or `default` for missing keys."
  ([config k]
   (get-optional config k nil))
  ([config k default]
   (get config k default)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

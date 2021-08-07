(ns lib.slf4j.mdc
  "MDC logging context."
  (:import
    (java.util Map)
    (org.slf4j MDC)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn add-context-map
  "Add keys/values from Clojure/Java map to logging context (as side-effect).
   When `ks` sequence provided then only these keys are taken."
  ([m]
   (cond
     ; Process clojure map with conversion of keys/values to strings.
     (map? m)
     (reduce-kv (fn [_ k v] (MDC/put (name k) (str v))), nil, m)

     ; Process java map without conversion of keys/values.
     (instance? Map m)
     (reduce (fn [_ k] (MDC/put k (.get ^Map m k))), nil, (.keySet ^Map m))

     ; Ignore non-maps.
     :else nil))
  ([m ks]
   (cond
     ; Process clojure map with conversion of keys/values to strings.
     (map? m)
     (reduce (fn [_ k] (when-some [v (m k)]
                         (MDC/put (name k) (str v))))
             nil, ks)

     ; Process java map without conversion of keys/values.
     (instance? Map m)
     (reduce (fn [_ k] (when-some [v (.get ^Map m k)]
                         (MDC/put k v)))
             nil, ks)

     ; Ignore non-maps.
     :else nil)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-context-map
  "Return a copy of the current thread's context map,
   with keys and values of type String.
   Returned value may be null."
  ^Map []
  (MDC/getCopyOfContextMap))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro with-map
  "Execute `body` wrapped with added context from map.
   Restore context map at the end."
  [m & body]
  `(let [context-map# ~m
         old-context# (MDC/getCopyOfContextMap)]
     (try
       (add-context-map context-map#)
       ~@body
       (finally
         (if old-context# (MDC/setContextMap old-context#)
                          (MDC/clear))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro with-keys
  "Execute `body` wrapped with added context for keys from map.
   Restore context map at the end."
  [m ks & body]
  `(let [context-map# ~m
         old-context# (MDC/getCopyOfContextMap)]
     (try
       (add-context-map context-map# ~ks)
       ~@body
       (finally
         (if old-context# (MDC/setContextMap old-context#)
                          (MDC/clear))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

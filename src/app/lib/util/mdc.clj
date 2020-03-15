(ns app.lib.util.mdc
  "MDC logging context."
  (:import
    (java.util Map)
    (org.slf4j MDC)))

(set! *warn-on-reflection* true)


(defn add-context-map
  "Add keys/values from Clojure/Java map to logging context (as side-effect)."
  [m]
  (cond
    ; Process clojure map with conversion of keys/values to strings.
    (map? m)
    (reduce-kv (fn [_ k v] (MDC/put (name k) (str v))), nil, m)

    ; Process java map without conversion of keys/values.
    (instance? Map m)
    (reduce (fn [_ k] (MDC/put k (.get ^Map m k))), nil, (.keySet ^Map m))

    ; Ignore non-maps.
    :else nil))


(defn get-context-map
  "Return a copy of the current thread's context map,
   with keys and values of type String.
   Returned value may be null."
  ^Map []
  (MDC/getCopyOfContextMap))


(defmacro wrap-with-map
  "Execute `body` wrapped with added context map.
   Restore context map at the end."
  [context-map & body]
  `(let [context-map# ~context-map
         old-context# (MDC/getCopyOfContextMap)]
     (try
       (add-context-map context-map#)
       ~@body
       (finally
         (if old-context# (MDC/setContextMap old-context#)
                          (MDC/clear))))))
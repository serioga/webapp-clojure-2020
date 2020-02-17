(ns app.lib.util.logging-context
  "MDC logging context."
  (:import
    (java.util Map)
    (org.slf4j MDC)))

(set! *warn-on-reflection* true)


(defn add-logging-context
  "Add keys/values from Clojure/Java map to logging context (as side-effect)."
  [m]
  (cond
    ; process clojure map with conversion of keys/values to strings
    (map? m)
    (reduce-kv (fn [_ k v] (MDC/put (name k) (str v))), nil, m)

    ; otherwise process java map without conversion of keys/values
    (instance? Map m)
    (reduce (fn [_ k] (MDC/put k (.get ^Map m k))), nil, (.keySet ^Map m))

    ; do nothing if not map
    :else nil))


(defn get-logging-context
  "Get logging contexts as Java (not Clojure!) map."
  ^Map []
  (MDC/getCopyOfContextMap))


(defmacro with-logging-context
  "Execute `body` wrapped with added logging context map.
   Restore logging context at the end."
  [context-map & body]
  `(let [context-map# ~context-map
         old-context# (MDC/getCopyOfContextMap)]
     (try
       (add-logging-context context-map#)
       ~@body
       (finally
         (if old-context#
           (MDC/setContextMap old-context#)
           (MDC/clear))))))
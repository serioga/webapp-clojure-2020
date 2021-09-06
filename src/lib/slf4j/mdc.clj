(ns lib.slf4j.mdc
  "MDC logging context utility."
  (:import (java.io Closeable)
           (org.slf4j MDC)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private noop-closeable
  (reify Closeable (close [_])))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ^Closeable put-closeable
  "Puts a diagnostic context value `v` as identified with the key `k`
   into the current thread's diagnostic context map.
   Returns a Closeable object who can remove key when close is called.
   The `k` cannot be null.
   If the `v` is null then nothing is put and noop Closeable returned."
  [k v]
  (if (some? v) (MDC/putCloseable k v), noop-closeable))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

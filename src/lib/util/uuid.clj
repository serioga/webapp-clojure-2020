(ns lib.util.uuid
  (:require [lib.clojure.core :as c])
  (:import (java.util UUID)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn from-string
  "Initialize UUID from string representation.
   Accept only zero-padded representation."
  [s]
  {:pre [(any? (c/assert-pred s (some-fn string? nil?) "UUID string"))]}
  (let [uuid (try (UUID/fromString s) (catch Throwable _))]
    (when (= s (str uuid))
      uuid)))

(comment
  (str (UUID/randomUUID))
  (from-string "123")
  (from-string "49230eb0-9e0c-4d5e-b22e-3bd022cc72d0")
  (UUID/fromString "4-9-4-b-3")
  (from-string "4-9-4-b-3")
  (from-string nil)
  (from-string [1 2 3]))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

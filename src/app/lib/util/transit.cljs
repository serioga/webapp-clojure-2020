(ns app.lib.util.transit
  (:require
    [cognitect.transit :as transit]))

(set! *warn-on-infer* true)


(defn read-transit-string [s]
  (transit/read (transit/reader :json) s))


(defn write-transit-string [o]
  (transit/write (transit/writer :json) o))

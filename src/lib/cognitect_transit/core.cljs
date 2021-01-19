(ns lib.cognitect-transit.core
  (:require [cognitect.transit :as transit]))

(set! *warn-on-infer* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn read-transit-string
  "Read a transit encoded string into ClojureScript values
   given :json reader."
  [s]
  (transit/read (transit/reader :json) s))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn write-transit-string
  "Encode an object into a transit string given :json writer."
  [o]
  (transit/write (transit/writer :json) o))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

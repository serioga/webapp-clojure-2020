(ns app.lib.util.transit
  (:require
    [cognitect.transit :as transit])
  (:import
    (java.io ByteArrayOutputStream ByteArrayInputStream)))

(set! *warn-on-reflection* true)


(defn read-transit-stream
  "Read data from input stream with transit bytes."
  [stream]
  (transit/read (transit/reader stream :json)))


(defn read-transit-string [^String s]
  (read-transit-stream (ByteArrayInputStream. (.getBytes s "UTF-8"))))


(defn ^:private write-transit [o out]
  (transit/write (transit/writer out :json) o))


(defn ^:private write-bytes ^bytes [o]
  (let [os (ByteArrayOutputStream.)]
    (write-transit o os)
    (.toByteArray os)))


(defn write-transit-string
  "Write data as transit string."
  [o]
  (String. (write-bytes o) "UTF-8"))

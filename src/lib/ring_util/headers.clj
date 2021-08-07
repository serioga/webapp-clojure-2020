(ns lib.ring-util.headers
  (:import (java.time Instant ZonedDateTime ZoneOffset)
           (java.time.format DateTimeFormatter)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn instant->http-date
  "RFC-1123 date string from `java.time.Instant`."
  [^Instant instant]
  (let [d (ZonedDateTime/ofInstant instant ZoneOffset/UTC)
        formatter DateTimeFormatter/RFC_1123_DATE_TIME]
    (.format formatter d)))

(comment
  (instant->http-date (Instant/now)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••


(ns app.lib.util.ring
  (:require
    [ring.util.response :as ring-response])
  (:import
    (java.time Instant ZonedDateTime ZoneOffset)
    (java.time.format DateTimeFormatter)))

(set! *warn-on-reflection* true)


(defn request-uri
  "Read path with query string from ring request."
  [request]
  (str (:uri request)
       (when-let [query (:query-string request)]
         (str "?" query))))


(defn url-for-path-in-request
  "Build URL for path with same scheme as request."
  [request path]
  (str (or (get-in request [:headers "x-forwarded-proto"])
           (-> request :scheme name))
       "://"
       (get-in request [:headers "host"])
       path))


(defn url-for-current-path
  "Absolute URL for path in request."
  [request]
  (url-for-path-in-request request (request-uri request)))


(defn- response-type-charset*
  ([body content-type]
   (response-type-charset* body content-type 200))
  ([body content-type status]
   (-> body
       (ring-response/response)
       (ring-response/content-type content-type)
       (ring-response/charset "utf-8")
       (ring-response/status status))))


(defn plain-text-response
  "Ring response with `text/plain` content type."
  ([text]
   (plain-text-response text 200))
  ([text status]
   (response-type-charset* text "text/plain" status)))


(defn xml-response
  "Ring response with `application/xml` content type."
  ([xml]
   (xml-response xml 200))
  ([xml status]
   (response-type-charset* xml "application/xml" status)))


(defn html-response
  "Ring response with `text/html` content type."
  ([html]
   (html-response html 200))
  ([html status]
   (response-type-charset* html "text/html" status)))


(defn instant->http-date
  "Format `java.time.Instant` as RFC-1123 datetime string."
  [^Instant instant]
  (let [d (ZonedDateTime/ofInstant instant ZoneOffset/UTC)
        formatter DateTimeFormatter/RFC_1123_DATE_TIME]
    (.format formatter d)))

(comment
  (instant->http-date (Instant/now)))

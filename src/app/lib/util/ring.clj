(ns app.lib.util.ring
  (:require
    [ring.util.response :as ring-response])
  (:import
    (java.time Instant ZonedDateTime ZoneOffset)
    (java.time.format DateTimeFormatter)))

(set! *warn-on-reflection* true)


(defn request-uri
  [request]
  (str (:uri request)
       (if-let [query (:query-string request)]
         (str "?" query))))


(defn url-for-path-in-request
  [request path]
  (str
    (or
      (get-in request [:headers "x-forwarded-proto"])
      (-> request :scheme name))
    "://"
    (get-in request [:headers "host"])
    path))


(defn url-for-current-path
  [request]
  (url-for-path-in-request request (request-uri request)))


(defn ^:private response-type-charset*
  ([body content-type]
   (response-type-charset* body content-type 200))
  ([body content-type status]
   (-> body
       (ring-response/response)
       (ring-response/content-type content-type)
       (ring-response/charset "utf-8")
       (ring-response/status status))))


(defn plain-text-response
  ([text]
   (plain-text-response text 200))
  ([text status]
   (response-type-charset* text "text/plain" status)))


(defn xml-response
  ([xml]
   (xml-response xml 200))
  ([xml status]
   (response-type-charset* xml "application/xml" status)))


(defn html-response
  ([html]
   (html-response html 200))
  ([html status]
   (response-type-charset* html "text/html" status)))


(defn set-cookie [response cookie-name cookie]
  (let [c (assoc cookie :path "/"
                        #_#_#_#_:http-only true
                            :same-site :strict)]
    (update response :cookies assoc cookie-name c)))


(defn remove-cookie [response cookie-name]
  (set-cookie response cookie-name {:value "removing_cookie_value..." :max-age -1}))


(defn get-cookie-value
  "Read cookie value from request."
  [request cookie-name]
  (get-in request [:cookies cookie-name :value]))


(defn instant->http-date
  [^Instant instant]
  (let [d (ZonedDateTime/ofInstant instant ZoneOffset/UTC)
        formatter DateTimeFormatter/RFC_1123_DATE_TIME]
    (.format formatter d)))

#_(comment
    (instant->http-date (Instant/now)))

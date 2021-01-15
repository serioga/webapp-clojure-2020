(ns lib.ring-util.response
  (:require
    [potemkin :refer [import-vars]]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(import-vars [lib.ring-util.cookie set-cookie remove-cookie])

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- response-type-charset*
  ([body content-type]
   (response-type-charset* body content-type 200))
  ([body content-type status]
   {:body body
    :headers {"Content-Type" (.concat ^String content-type "; charset=utf-8")}
    :status status}))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn plain-text
  "Ring response with `text/plain` type."
  ([body]
   (plain-text body 200))
  ([body status]
   (response-type-charset* body "text/plain" status)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn xml
  "Ring response with `application/xml` type."
  ([body]
   (xml body 200))
  ([body status]
   (response-type-charset* body "application/xml" status)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn html
  "Ring response with `text/html` type."
  ([body]
   (html body 200))
  ([body status]
   (response-type-charset* body "text/html" status)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

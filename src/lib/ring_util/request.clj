(ns lib.ring-util.request
  (:require [lib.clojure-string.core :as string]
            [lib.ring-util.cookie]
            [potemkin :refer [import-vars]]
            [ring.util.request]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(import-vars [ring.util.request request-url])

(import-vars [lib.ring-util.cookie get-cookie-value])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn request-uri
  "Extract path with query string from ring request."
  [request]
  (let [query (:query-string request)]
    (cond-> (:uri request)
      query (string/concat "?" query))))

(comment
  (request-uri {:uri "http://localhost/"})
  (request-uri {:uri "http://localhost/" :query-string "a=1"}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn url-for-path
  "Build URL for path with same scheme like incoming request."
  [request path]
  (let [headers (:headers request)]
    (string/concat (or (headers "x-forwarded-proto")
                       (-> request :scheme name))
                   "://" (headers "host") path)))

(comment
  (url-for-path {:headers {"x-forwarded-proto" "http", "host" "localhost"}} "/")
  (url-for-path {:headers {"host" "localhost"}, :scheme "http"} "/"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn url-for-current-path
  "Absolute URL for the request."
  [request]
  (url-for-path request (request-uri request)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- anonymize-ip
  "Replace last octet in IP address with zero."
  [ip]
  (some-> (string/drop-end ip #(case % (\. \:) false true))
          (string/concat "0")))

(defn client-ip
  "Read client IP address from ring request.
   Respects proxy headers. Anonymizes IP address."
  [request]
  (-> (or (some-> request :headers (get "x-forwarded-for") (string/take-before ","))
          (:remote-addr request))
      (anonymize-ip)))

(comment
  (anonymize-ip "127.0.0.122")
  (anonymize-ip "2001:db8::ff00:42:125")
  (anonymize-ip nil)
  (client-ip {:remote-addr "127.0.0.122"})
  (client-ip {:headers {"x-forwarded-for" "127.0.0.122"}})
  (client-ip {:headers {"x-forwarded-for" "127.0.0.122,127.0.0.122"}}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

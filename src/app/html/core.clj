(ns app.html.core
  (:require [clojure.java.io :as io]
            [rum.core :as rum])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn render-page
  "Renders hiccup to HTML page string."
  [hiccup]
  (str "<!DOCTYPE html>\n"
       (rum/render-static-markup hiccup)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn include-css
  "Hiccup to include external CSS."
  [href]
  [:link {:type "text/css", :href href, :rel "stylesheet"}])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn include-js
  "Hiccup to include external JS."
  ([src]
   (include-js src nil))
  ([src, defer-or-async]
   (list [:script {:type "text/javascript"
                   :src src
                   :defer (= :defer defer-or-async)
                   :async (= :async defer-or-async)}])))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn static-uri-with-hash
  "Attach hash parameter to URI of static resource."
  [uri]
  (let [path (str "public" uri)
        content (slurp (or (io/resource path)
                           (throw (ex-info (print-str "Missing static resource" (pr-str path))
                                           {:name name :resource-path path}))))
        content-hash (DigestUtils/sha256Hex content)]
    (str uri "?" content-hash)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

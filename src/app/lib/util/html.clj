(ns app.lib.util.html
  (:require
    [clojure.java.io :as io])
  (:import
    (org.apache.commons.codec.digest DigestUtils)))

(set! *warn-on-reflection* true)


(defn include-css
  [href]
  [:link {:type "text/css", :href href, :rel "stylesheet"}])


(defn include-js
  ([src]
   (include-js src nil))
  ([src, defer-or-async]
   (list
     [:script
      {:type "text/javascript"
       :src src
       :defer (= :defer defer-or-async)
       :async (= :async defer-or-async)}])))


(defn static-uri-with-hash
  [uri]
  (let [path (str "public" uri)
        content (slurp (or (io/resource path)
                           (throw (ex-info (print-str "Missing static resource" (pr-str path))
                                           {:name name :resource-path path}))))
        hash (DigestUtils/sha256Hex content)]
    (str uri "?" hash)))


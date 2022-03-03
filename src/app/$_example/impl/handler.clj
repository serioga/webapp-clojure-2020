(ns app.$-example.impl.handler
  (:require [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti route-path
  "Returns route path for tag."
  {:arglists '([route-tag])}
  c/first-arg)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti example-handler
  "Handle ring request by route-tag."
  :route-tag)

(c/add-method example-handler nil (constantly nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

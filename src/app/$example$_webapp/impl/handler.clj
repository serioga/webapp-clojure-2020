(ns app.$example$-webapp.impl.handler
  (:require [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti example-handler
  "Handle ring request by route-tag."
  :route-tag)

(e/add-method example-handler nil (constantly nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

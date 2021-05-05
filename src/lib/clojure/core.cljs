(ns lib.clojure.core
  {:clj-kondo/config {:linters {:missing-docstring {:level :off}}}}
  (:require [lib.clojure.lang :as lang]))

(set! *warn-on-infer* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def add-method lang/add-method)
(def invoke,,,, lang/invoke)
(def tested-by, lang/tested-by)
(def unwrap-fn, lang/unwrap-fn)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

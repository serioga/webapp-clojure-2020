(ns lib.clojure.core
  {:clj-kondo/config {:linters {:missing-docstring {:level :off}}}}
  (:require [lib.clojure.lang :as lang]))

(set! *warn-on-infer* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def add-method lang/add-method)
(def first-arg, lang/first-arg)
(def second-arg lang/second-arg)
(def invoke,,,, lang/invoke)
(def asserted,, lang/asserted)
(def unwrap-fn, lang/unwrap-fn)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

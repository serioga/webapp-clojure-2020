(ns lib.util.secret
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as test]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(declare secret?)

(deftype Secret [value]
  Object
  (equals [_ obj] (and (secret? obj) (= value (.value ^Secret obj))))
  (toString [_] "******"))

(test/deftest deftype-test
  (test/are [expr result] (= result expr)
    (str (->Secret "secret value")) #_=> "******"
    (= (->Secret "secret value") (->Secret "secret value")) #_=> true))

(comment
  (str (->Secret "xxx"))
  (str "My secret: " (->Secret "xxx"))
  (pr-str (->Secret "xxx"))
  (.value (->Secret "xxx"))
  (= (->Secret "xxx") (->Secret "xxx"))
  (= (->Secret nil) (->Secret nil)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn secret?
  "Test if `x` is a secret."
  [x]
  (instance? Secret x))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(s/def ::spec
  (s/or :string string? :secret secret?))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn read-secret
    "Read value from secret."
    [value]
    (if (secret? value)
      (.value ^Secret value)
      value))

  (test/are [expr result] (= result expr)
    (read-secret (->Secret "xxx")) #_=> "xxx"
    (read-secret "xxx"),,,,,,,,,,, #_=> "xxx"))

(comment
  (read-secret "xxx")
  (read-secret (->Secret "xxx"))
  (test/test-var #'read-secret))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

(defn read-secret
  "Read value from secret."
  {:test #(test/are [form] form
            (= "xxx" (read-secret (->Secret "xxx")))
            (= "xxx" (read-secret "xxx")))}
  [value]
  (if (secret? value)
    (.value ^Secret value)
    value))

(comment
  (read-secret "xxx")
  (read-secret (->Secret "xxx"))
  (test/test-var #'read-secret))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest secret-test
  (let [v "secret value"
        s (->Secret v)]
    (test/are [form] form
      (= "******" (str s))
      (= v,,,,,,, (read-secret s))
      (= s,,,,,,, (->Secret v)))))

(comment
  (secret-test))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

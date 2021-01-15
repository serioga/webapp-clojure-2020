(ns lib.util.secret
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :as test]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(deftype Secret [value]
  Object
  (toString [_] "******"))

(comment
  (str (->Secret "xxx"))
  (str "My secret: " (->Secret "xxx"))
  (pr-str (->Secret "xxx"))
  (.value (->Secret "xxx"))
  (= (->Secret "xxx") (->Secret "xxx")))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn secret?
  "Test if `x` is a secret."
  [x]
  (instance? Secret x))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(s/def ::spec
  (s/or :string string? :secret secret?))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn read-secret
  "Read value from secret."
  {:test (fn []
           (test/are [expected actual] (= expected actual)
             "xxx" (read-secret (->Secret "xxx"))
             "xxx" (read-secret "xxx")))}
  [value]
  (if (secret? value)
    (.value ^Secret value)
    value))

(comment
  (read-secret "xxx")
  (read-secret (->Secret "xxx"))
  (test/test-var #'read-secret))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest secret-test
  (let [v "secret value"
        test (->Secret v)]
    (test/are [expected actual] (= expected actual)
      "******" (str test)
      v,,,,,,, (read-secret test))))

(comment
  (secret-test))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

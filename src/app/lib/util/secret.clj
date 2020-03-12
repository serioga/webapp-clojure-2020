(ns app.lib.util.secret
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :as t]))

(set! *warn-on-reflection* true)


(deftype Secret [value]
  Object
  (toString [_] "******"))

#_(comment
    (str (->Secret "xxx"))
    (str "My secret: " (->Secret "xxx"))
    (pr-str (->Secret "xxx"))
    (.value (->Secret "xxx"))
    (= (->Secret "xxx") (->Secret "xxx")))


(defn secret?
  "Test if `x` is a secret."
  [x]
  (instance? Secret x))


(s/def ::spec
  (s/or :string string? :secret secret?))


(defn read-secret
  "Read value from secret."
  {:test (fn []
           (t/is (= (read-secret (->Secret "xxx")), "xxx"))
           (t/is (= (read-secret "xxx"), "xxx")))}
  [value]
  (if (secret? value)
    (.value ^Secret value)
    value))

#_(comment
    (read-secret "xxx")
    (read-secret (->Secret "xxx"))
    (t/test-var #'read-secret))


(t/deftest secret-test
  (let [v "secret value"
        test (->Secret v)]
    (t/is (= "******"
             (str test)))
    (t/is (= v
             (read-secret test)))))

#_(comment
    (test'secret))

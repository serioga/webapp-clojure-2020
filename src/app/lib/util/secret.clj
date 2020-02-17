(ns app.lib.util.secret
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :as t])
  (:import
    (java.io Writer)))

(set! *warn-on-reflection* true)


(defrecord Secret [value]
  Object
  (toString [_] "<secret>"))


(defmethod print-method Secret
  [_ ^Writer writer]
  (.write writer "#<Secret>"))

#_(comment
    (str (->Secret "xxx"))
    (pr-str (->Secret "xxx"))
    (.value (->Secret "xxx"))
    (= (->Secret "xxx") (->Secret "yyy")))


(defn secret?
  [x]
  (instance? Secret x))


(s/def ::spec
  (s/or :string string? :secret secret?))


(defn read-secret
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


(t/deftest test-secret
  (let [v "secret value"
        test (->Secret v)]
    (t/is (= (str test), "<secret>"))
    (t/is (= (pr-str test), "#<Secret>"))
    (t/is (= (.value ^Secret test), v))
    (t/is (= (->Secret v), test))
    (t/is (not= (->Secret (str v "1")), test))))

#_(comment
    (test-secret))

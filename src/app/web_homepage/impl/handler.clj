(ns app.web-homepage.impl.handler)

(set! *warn-on-reflection* true)


(defmulti homepage-handler :route-tag)


(defmethod homepage-handler nil
  [_]
  nil)

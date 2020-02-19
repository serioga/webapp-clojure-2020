(ns app.web-example.impl.handler)

(set! *warn-on-reflection* true)


(defmulti example-handler :route-tag)


(defmethod example-handler nil
  [_]
  nil)

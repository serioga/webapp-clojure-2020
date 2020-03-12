(ns app.web-example.impl.handler)

(set! *warn-on-reflection* true)


(defmulti example-handler
  "Handle ring request by route-tag."
  :route-tag)


(defmethod example-handler nil
  [_]
  nil)

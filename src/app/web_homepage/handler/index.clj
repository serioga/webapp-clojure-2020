(ns app.web-homepage.handler.index
  (:require
    [app.lib.util.ring :as ring-util]
    [app.web-homepage.impl.handler :as impl]))

(set! *warn-on-reflection* true)


(defmethod impl/homepage-handler :route/index
  [request]
  (ring-util/plain-text-response (str (:method request) " Homepage")))

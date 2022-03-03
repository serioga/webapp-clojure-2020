(ns app.$-example.handler.index
  (:require [app.$-example.impl.handler :as impl]
            [app.$-example.impl.html :as html]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(c/add-method impl/route-path :route/index (constantly "/"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod impl/example-handler :route/index
  [{:keys [route-tag/path-for-route]}]
  (-> [:html [:head
              [:title "Homepage"]
              (html/include-app-css)]
       [:body
        [:h1 "Examples"]
        [:ul
         [:li [:a {:href (path-for-route :route/example-react)} "React Component"]]
         [:li [:a {:href (path-for-route :route/example-database)} "SQL Database"]]
         [:li [:a {:href (path-for-route :route/example-path-param {:name "Test Name"
                                                                    :value "Test Value"})} "Path Parameter"]]]]]
      (html/response)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

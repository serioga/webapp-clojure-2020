(ns app.web-example.handler.index
  (:require
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html :as html]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

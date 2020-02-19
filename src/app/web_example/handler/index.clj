(ns app.web-example.handler.index
  (:require
    [app.lib.util.html :as html]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


(defmethod impl/example-handler :route/index
  [request]
  (html-page/response
    [:html [:head
            [:title "Homepage"]
            (html/include-css html-page/styles-css-uri)]
     [:body
      [:h1 "Examples"]
      [:ul
       [:li [:a {:href "/example-react"} "React Component"]]
       [:li [:a {:href "/example-database"} "SQL Database"]]]]]))

(ns app.web-example.handler.example-path-param
  (:require
    [app.lib.util.html :as html]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


(defmethod impl/example-handler :route/example-path-param
  [request]
  (let [title "Path Parameter example"
        {:keys [name, value]} (:params request)]
    (html-page/response
      [:html [:head
              [:title title]
              (html/include-css html-page/styles-css-uri)]
       [:body
        [:h1 title]
        [:div
         [:div.border.p-2.mb-4
          [:tt (str (clojure.walk/prewalk-replace {'name name 'value value}
                                                  '(path-for-route :route/example-path-param
                                                                   {:name name :value value})))]]
         [:ul
          [:li "Name: " [:tt.bg-gray-200 name]]
          [:li "Value: " [:tt.bg-gray-200 value]]]
         (html-page/link-to-index)]]])))

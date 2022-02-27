(ns app.$-example.handler.example-path-param
  (:require [app.$-example.impl.handler :as impl]
            [app.$-example.impl.html :as html]
            [clojure.walk :as walk]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod impl/example-handler :route/example-path-param
  [request]
  (let [title "Path Parameter example"
        {name-param :name value-param :value} (:params request)]
    (-> [:html [:head
                [:title title]
                (html/include-app-css)]
         [:body
          [:h1 title]
          [:div
           [:div.border.p-2.mb-4
            [:tt (str (walk/prewalk-replace {'name-param name-param 'value-param value-param}
                                            '(path-for-route :route/example-path-param {:name name-param :value value-param})))]]
           [:ul
            [:li "Name: " [:tt.bg-gray-200 name-param]]
            [:li "Value: " [:tt.bg-gray-200 value-param]]]
           (html/link-to-index)]]]
        (html/response))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.web-example.handler.example-path-param
  (:require [app.web-example.impl.handler :as impl]
            [app.web-example.impl.html :as html]
            [clojure.walk :as walk]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod impl/example-handler :route/example-path-param
  [request]
  (let [title "Path Parameter example"
        {:keys [name, value]} (:params request)]
    (-> [:html [:head
                [:title title]
                (html/include-app-css)]
         [:body
          [:h1 title]
          [:div
           [:div.border.p-2.mb-4
            [:tt (str (walk/prewalk-replace {'name name 'value value}
                                            '(path-for-route :route/example-path-param
                                                             {:name name :value value})))]]
           [:ul
            [:li "Name: " [:tt.bg-gray-200 name]]
            [:li "Value: " [:tt.bg-gray-200 value]]]
           (html/link-to-index)]]]
        (html/response))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.web-example.handler.example-database
  (:require
    [app.database.core :as db]
    [app.lib.util.html :as html]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]
    [clojure.pprint :as pprint]))

(set! *warn-on-reflection* true)


(defmethod impl/example-handler :route/example-database
  [_]
  (let [title "SQL Database example"
        result (db/with-read-only db/example-user--select)]
    (html-page/response
      [:html [:head
              [:title title]
              (html/include-css html-page/styles-css-uri)]
       [:body
        [:h1 title]
        [:div
         [:pre
          (with-out-str (pprint/pprint result))]
         (html-page/link-to-index)]]])))

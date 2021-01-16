(ns app.web-example.handler.example-database
  (:require
    [app.database.core :as db]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html :as html]
    [clojure.pprint :as pprint]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod impl/example-handler :route/example-database
  [_]
  (let [title "SQL Database example"
        result (db/ro db/example.list-user)]
    (-> [:html [:head
                [:title title]
                (html/include-app-css)]
         [:body
          [:h1 title]
          [:div
           [:pre (with-out-str (pprint/pprint result))]
           (html/link-to-index)]]]
        (html/response))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.$example$-webapp.handler.example-database
  (:require [app.$example$-webapp.impl.handler :as impl]
            [app.$example$-webapp.impl.html :as html]
            [app.database.core :as db]
            [clojure.pprint :as pprint]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod impl/example-handler :route/example-database
  [_]
  (let [title "SQL Database example"
        result (db/example-user--select-all)]
    (-> [:html [:head
                [:title title]
                (html/include-app-css)]
         [:body
          [:h1 title]
          [:div
           [:pre (with-out-str (pprint/pprint result))]
           (html/link-to-index)]]]
        (html/response))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.$-example.handler.example-database
  (:require [app.$-example.impl.handler :as impl]
            [app.$-example.impl.html :as html]
            [app.database.core :as db]
            [clojure.pprint :as pprint]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(c/add-method impl/route-path :route/example-database (constantly "/example-database"))

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

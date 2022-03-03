(ns app.$-example.handler.example-react
  (:require [app.$-example.impl.handler :as impl]
            [app.$-example.impl.html :as html]
            [app.rum.mount :as rum.mount]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(c/add-method impl/route-path :route/example-react (constantly "/example-react"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; TODO: Deferred JS loading in release.

(defmethod impl/example-handler :route/example-react
  [request]
  (let [[components!, mount-component] (rum.mount/init-mounter request)
        title "React Component example"]
    (-> [:html [:head
                [:title title]
                (html/include-app-css)]
         [:body
          [:h1 title]
          (mount-component :react-component/hello-world {:name "World"})
          (html/link-to-index)
          (rum.mount/react-mount-data-js @components!)
          (html/include-app-js)]]
        (html/response))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

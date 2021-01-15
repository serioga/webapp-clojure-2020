(ns app.web-example.handler.example-react
  (:require
    [app.lib.util.html :as html]
    [app.rum.mount :as rum-mount]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


; TODO: Deferred JS loading in release.

(defmethod impl/example-handler :route/example-react
  [request]
  (let [[var'components, mount-component] (rum-mount/init-mounter request)
        title "React Component example"]
    (-> [:html [:head
                [:title title]
                (html/include-css html-page/styles-css-uri)]
         [:body
          [:h1 title]
          (mount-component :react-component/hello-world {:name "World"})
          (html-page/link-to-index)
          (rum-mount/react-mount-data-js @var'components)
          (html/include-js "/app/example/main.js")]]
        (html-page/response))))


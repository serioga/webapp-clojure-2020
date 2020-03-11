(ns app.web-example.handler.example-react
  (:require
    [app.lib.react.mount :as react-mount]
    [app.lib.util.html :as html]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


; TODO Deferred JS loading in release

(defmethod impl/example-handler :route/example-react
  [request]
  (let [[ref'registry, mount-component] (react-mount/new-registry-mounter request)
        title "React Component example"]
    (-> [:html [:head
                [:title title]
                (html/include-css html-page/styles-css-uri)]
         [:body
          [:h1 title]
          (mount-component :react-component/hello-world {:name "World"})
          (html-page/link-to-index)
          (react-mount/react-mount-data-js @ref'registry)
          (html/include-js "/app/example/main.js")]]
        (html-page/response))))


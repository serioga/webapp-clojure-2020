(ns app.web-example.handler.index
  (:require
    [app.lib.react.mount :as react-mount]
    [app.lib.util.html :as html]
    [app.web-example.impl.handler :as impl]
    [app.web-example.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


; TODO Deferred JS loading in release

(defmethod impl/example-handler :route/index
  [request]
  (let [[registry, mount-component]
        (react-mount/new-registry-mounter request)]
    (html-page/response
      [:html [:head
              [:title "Homepage"]
              (html/include-css html-page/styles-css-uri)]
       [:body
        [:h1 "Homepage"]
        [:p "Request method " (:request-method request)]
        [:ul
         [:li [:a {:href "/ru"} "RU"]]
         [:li [:a {:href "/en"} "EN"]]]
        (mount-component :react-component/hello-world {:name "World"})
        (react-mount/react-mount-data-js @registry)
        (html/include-js "/app/example/main.js")]])))

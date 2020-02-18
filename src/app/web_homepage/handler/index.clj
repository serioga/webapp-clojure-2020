(ns app.web-homepage.handler.index
  (:require
    [app.lib.react.mount :as react-mount]
    [app.lib.util.html :as html]
    [app.web-homepage.impl.handler :as impl]
    [app.web-homepage.impl.html-page :as html-page]))

(set! *warn-on-reflection* true)


; TODO Deferred JS loading in release

(defmethod impl/homepage-handler :route/index
  [request]
  (let [[registry, mount-component]
        (react-mount/new-registry-mounter request)]
    (html-page/response
      [:html [:head
              [:title "Homepage"]]

       [:body (str (:method request) " Homepage")
        [:ul
         [:li [:a {:href "/ru"} "RU"]]
         [:li [:a {:href "/en"} "EN"]]]
        (mount-component :react-component/hello-world {:name "World"})
        (react-mount/react-mount-data-js @registry)
        (html/include-js "/app/homepage/main.js")]])))

(ns app.web-example.impl.html-page
  (:require
    [app.lib.util.html :as html]
    [lib.ring-util.response :as ring-response]
    [mount.core :as mount]
    [rum.core :as rum]))

(set! *warn-on-reflection* true)


(mount/defstate styles-css-uri
  "Path to CSS with hash parameter"
  {:on-reload :noop}
  :start (html/static-uri-with-hash "/app/example/main.css"))


(defn- render-html
  [hiccup]
  (str "<!DOCTYPE html>\n"
       (rum/render-static-markup hiccup)))


(defn response
  "Render hiccup to HTML response."
  [hiccup]
  (-> hiccup
      (render-html)
      (ring-response/html)))


(defn link-to-index
  "Build hiccup for the link to index page."
  []
  [:p.mt-4 [:a {:href "/"} "< index"]])

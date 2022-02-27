(ns app.$-example.impl.html
  (:require [app.html.core :as html]
            [lib.ring-util.response :as ring.response']
            [mount.core :as mount]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(mount/defstate styles-css-uri
  "Path to CSS with hash parameter"
  {:on-reload :noop}
  :start (html/static-uri-with-hash "/app/example/main.css"))

(defn include-app-css
  "Hiccup including main.css."
  []
  (html/include-css styles-css-uri))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn include-app-js
  "Hiccup including main.js."
  []
  (html/include-js "/app/example/main.js"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn link-to-index
  "Build hiccup for the link to index page."
  []
  [:p.mt-4 [:a {:href "/"} "< index"]])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn response
  "Render hiccup to HTML response."
  [hiccup]
  (-> hiccup
      (html/render-page)
      (ring.response'/html)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(ns app.web-homepage.impl.html-page
  (:require
    [app.lib.util.ring :as ring-util]
    [rum.core :as rum]))

(set! *warn-on-reflection* true)


(defn render-html
  [hiccup]
  (str "<!DOCTYPE html>\n"
    (rum/render-static-markup hiccup)))


(defn response
  [hiccup]
  (-> hiccup
    (render-html)
    (ring-util/html-response)))


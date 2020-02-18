(ns app.lib.util.html)

(set! *warn-on-reflection* true)


(defn include-css
  [href]
  [:link {:type "text/css", :href href, :rel "stylesheet"}])


(defn include-js
  ([src]
   (include-js src nil))
  ([src, defer-or-async]
   (list
     [:script
      {:type "text/javascript"
       :src src
       :defer (= :defer defer-or-async)
       :async (= :async defer-or-async)}])))


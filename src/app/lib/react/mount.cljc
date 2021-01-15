(ns app.lib.react.mount
  #?(:clj  (:require
             [clojure.string :as str])
     :cljs (:require
             [rum.core :as rum]))
  (:require
    [app.lib.react.component :as react-component]
    [lib.cognitect-transit.core :as transit]))

#?(:clj (set! *warn-on-reflection* true) :cljs (set! *warn-on-infer* true))


(def ^:private data-js-var "appReactMountData")


#?(:cljs
   (defn mount-all
     "Mount all components with data from server."
     []
     (let [components (some-> (aget js/window data-js-var)
                              (transit/read-transit-string))]
       (js-delete js/window data-js-var)
       (doseq [comp-data components]
         (let [instance-id (react-component/instance-id comp-data)]
           (rum/hydrate (react-component/create-component comp-data)
                        (js/document.getElementById (name instance-id))))))))


; TODO: Push state.

#?(:clj
   (defn mount-component
     "Hiccup-style element with pre-rendered react component.
      All react components are registered in registry to be added
      in page HTML later."
     ([var'registry, comp-id]
      (mount-component var'registry :div comp-id nil))

     ([var'registry, comp-id, comp-data]
      (mount-component var'registry :div comp-id comp-data))

     ([var'registry, tag, comp-id, comp-data]
      (let [comp-data (-> comp-data
                          (react-component/set-component-id comp-id))]
        (swap! var'registry conj comp-data)
        [tag
         {:id (react-component/instance-id comp-data)}
         (react-component/create-component comp-data)]))))


#?(:clj
   (defn new-registry-mounter
     "Provide mounting function registry."
     [_]
     (let [var'registry (atom [])]
       [var'registry (partial mount-component var'registry)])))


#?(:clj
   (defn react-mount-data-js
     "Hiccup for JS with data for mounting react components."
     [react-data]
     [:script
      {:dangerouslySetInnerHTML
       {:__html
        (str "window." data-js-var "=`"
             (-> (transit/write-transit-string react-data)
                 (str/replace "\\" "\\\\")
                 (str/replace "`" "\\`"))
             "`;")}}]))

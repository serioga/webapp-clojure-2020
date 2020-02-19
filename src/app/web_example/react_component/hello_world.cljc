(ns app.web-example.react-component.hello-world
  "Example react component, modification of
   https://github.com/tonsky/rum/blob/gh-pages/examples/rum/examples/local_state.cljc"
  (:require
    [app.lib.react.component :as impl]
    [rum.core :as rum]))

#?(:clj (set! *warn-on-reflection* true) :cljs (set! *warn-on-infer* true))


(rum/defcs hello-world < (rum/local 0)
  [state name]
  (let [*count (:rum/local state)]
    [:div
     {:style {"-webkit-user-select" "none"
              "cursor" "pointer"}
      :on-click (fn [_] (swap! *count inc))}
     (str "Hello, " name ": " @*count " clicks.")]))


(defmethod impl/create-component :react-component/hello-world
  [{:keys [name]}]
  (hello-world name))

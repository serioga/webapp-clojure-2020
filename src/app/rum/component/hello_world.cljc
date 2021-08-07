(ns app.rum.component.hello-world
  "Example react component, modification of
   https://github.com/tonsky/rum/blob/gh-pages/examples/rum/examples/local_state.cljc"
  (:require [app.rum.impl.component :as impl]
            [lib.clojure.core :as e]
            [rum.core :as rum]))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(rum/defcs hello-world
  "Example react component."
  < (rum/local 0)
  [state name]
  (let [*count (:rum/local state)]
    [:div
     {:style {"-webkit-user-select" "none"
              "cursor" "pointer"}
      :on-click (fn [_] (swap! *count inc))}
     (str "Hello, " name ": " @*count " clicks.")]))

(e/add-method impl/create-component :react-component/hello-world
              (comp hello-world :name))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

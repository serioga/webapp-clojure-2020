(ns app.lib.react.mixin.local
  "Similar to rum/local but but with function of state as initial value."
  (:require
    [rum.core :as rum]))

(defn local-mixin
  "Mixin constructor. Adds an atom to component’s state that can be used to keep stuff
   during component’s lifecycle. Component will be re-rendered if atom’s value changes.
   Atom is stored under user-provided key or under `:rum/local` by default"
  ([initial-fn] (local-mixin initial-fn :rum/local))
  ([initial-fn key]
   {:will-mount
    #?(:clj
       (fn [state]
         (assoc state key (atom (initial-fn state))))

       :cljs
       (fn [state]
         (let [var'local-state (atom (initial-fn state))
               component (:rum/react-component state)]
           (add-watch var'local-state key
             (fn [_ _ _ _]
               (rum/request-render component)))
           (assoc state key var'local-state))))}))

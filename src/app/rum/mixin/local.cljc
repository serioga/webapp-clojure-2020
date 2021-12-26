(ns app.rum.mixin.local
  "Similar to rum/local but but with function of state as initial value."
  #?(:cljs (:require [rum.core :as rum])))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn local-mixin
  "Mixin constructor. Adds an atom to component’s state that can be used to keep stuff
   during component’s lifecycle. Component will be re-rendered if atom’s value changes.
   Atom is stored under user-provided key or under `:rum/local` by default"
  ([init-state] (local-mixin init-state :rum/local))
  ([init-state k]
   {:will-mount #?(:clj  (fn [state]
                           (assoc state k (atom (init-state state))))

                   :cljs (fn [state]
                           (let [local! (atom (init-state state))
                                 component (:rum/react-component state)]
                             (add-watch local! k (fn [_ _ _ _] (rum/request-render component)))
                             (assoc state k local!))))}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

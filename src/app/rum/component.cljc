(ns app.rum.component
  (:require [app.rum.impl.component :as impl]))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn create-component
  "Component constructor by ID keyword in `data`."
  [data]
  (impl/create-component data))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn component-id
  "Get component ID."
  [data]
  (:app.rum/component-id data))

(defn set-component-id
  "Set component ID."
  [data comp-id]
  (assoc data :app.rum/component-id comp-id))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn instance-id
  "Instance ID to differentiate several components with same component.
  Optional, defaults to `component-id`."
  [data]
  (or (::instance-id data)
      (component-id data)))

(defn set-instance-id
  "Set component instance ID."
  [data instance-id]
  (assoc data ::instance-id instance-id))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

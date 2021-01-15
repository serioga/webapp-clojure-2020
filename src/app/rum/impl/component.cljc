(ns app.rum.impl.component)

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti create-component
  "Component constructor by ID keyword."
  :app.rum/component-id)

(defmethod create-component :default
  [data]
  (println "Calling default `create-component` for" data))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

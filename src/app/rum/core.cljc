(ns app.rum.core

  "Setup rum components."

  (:require ; components
    [app.rum.component.hello-world]))

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

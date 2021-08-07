(ns app.$example$-webapp.main
  ;; React components
  (:require [app.rum.core])
  ;; Imports
  (:require [app.rum.mount :as rum-mount]))

(set! *warn-on-infer* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

#_(defn ^:dev/after-load ^:private teardown
    []
    (println "reloading page...")
    (.reload (-> js/window .-location) true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(enable-console-print!)

(rum-mount/mount-all)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

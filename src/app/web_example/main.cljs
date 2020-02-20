(ns app.web-example.main
  (:require
    ; react components
    [app.web-example.config.react-components]
    ; imports
    [app.lib.react.mount :as react-mount]))

(set! *warn-on-infer* true)

(enable-console-print!)


(react-mount/mount-all)


#_(defn ^:dev/after-load ^:private teardown
    []
    (println "reloading page...")
    (.reload (-> js/window .-location) true))


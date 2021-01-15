(ns app.web-example.main
  (:require ; react components
    [app.rum.core])
  (:require
    [app.rum.mount :as rum-mount]))

(set! *warn-on-infer* true)

(enable-console-print!)


(rum-mount/mount-all)


#_(defn ^:dev/after-load ^:private teardown
    []
    (println "reloading page...")
    (.reload (-> js/window .-location) true))


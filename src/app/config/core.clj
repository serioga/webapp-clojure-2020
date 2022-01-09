(ns app.config.core
  (:require [lib.clojure.core :as c]
            [lib.config.core :as config]
            [mount.core :as mount]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(mount/defstate optional
  "Get optional value from global app-config.
   Return `nil` or `default` for missing keys."
  {:arglists '([key] [key default]) :on-reload :noop}
  :start (let [app-config (-> (::app-config (mount/args)) (c/assert map?))]
           (partial config/get-optional app-config)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(mount/defstate required
  "Get required value from global app-config.
   Raise exception for missing keys."
  {:arglists '([key]) :on-reload :noop}
  :start (let [app-config (-> (::app-config (mount/args)) (c/assert map?))]
           (partial config/get-required app-config)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

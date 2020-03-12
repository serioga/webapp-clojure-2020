(ns app.config.core
  (:require
    [app.lib.config.core :as config]
    [mount.core :as mount]
    [taoensso.truss :as truss]))

(set! *warn-on-reflection* true)


(mount/defstate ^{:on-reload :noop
                  :doc "Get optional value from global app-config.
                        Return `nil` or `default` for missing keys."
                  :arglists '([key] [key default])}
  optional
  :start (let [app-config (truss/have! map? (::app-config (mount/args)))]
           (partial config/get-optional app-config)))


(mount/defstate ^{:on-reload :noop
                  :doc "Get required value from global app-config.
                        Raise exception for missing keys."
                  :arglists '([key])}
  required
  :start (let [app-config (truss/have! map? (::app-config (mount/args)))]
           (partial config/get-required app-config)))


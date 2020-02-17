(ns app.config.core
  (:require
    [app.lib.config.core :as config]
    [mount.core :as mount]
    [taoensso.truss :as truss]))

(set! *warn-on-reflection* true)


(mount/defstate ^{:on-reload :noop} optional
  :start
  (let [app-config (truss/have! map? (::app-config (mount/args)))]
    (partial config/get-optional app-config)))


(mount/defstate ^{:on-reload :noop} required
  :start
  (let [app-config (truss/have! map? (::app-config (mount/args)))]
    (partial config/get-required app-config)))


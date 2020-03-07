(ns app.database.core
  (:require
    [app.database.hugsql :as hugsql]
    [mount.core :as mount]
    [next.jdbc :as jdbc]
    [taoensso.truss :as truss])
  (:import
    (java.sql Connection)))

(set! *warn-on-reflection* true)


; Database connection

(mount/defstate
  ^{:on-reload :noop
    :tag Connection
    :doc "Creates a connection to a database using read-write `data-source`."
    :arglists '([] [options])}
  get-read-write-connection
  :start
  (let [ref'data-source (truss/have! future?
                          (::ref'data-source-read-write (mount/args)))]
    (fn get-read-write-connection
      ([]
       (get-read-write-connection {}))
      ([options]
       (jdbc/get-connection @ref'data-source options)))))


(mount/defstate
  ^{:on-reload :noop
    :tag Connection
    :doc "Creates a connection to a database using read-only `data-source`."
    :arglists '([] [options])}
  get-read-only-connection
  :start
  (let [ref'data-source (truss/have! future?
                          (::ref'data-source-read-only (mount/args)))]
    (fn get-read-only-connection
      ([]
       (get-read-only-connection {}))
      ([options]
       (jdbc/get-connection @ref'data-source options)))))


; with-open connection helpers

(defn with-read-write
  "Execute single HugSQL query with auto opened read-write connection."
  ([query-fn] (with-read-write query-fn {} {}))
  ([query-fn param-data] (with-read-write query-fn param-data {}))
  ([query-fn param-data opts]
   (with-open [conn (get-read-write-connection)]
     (query-fn conn param-data opts))))


(defn with-read-only
  "Execute single HugSQL query with auto opened read-only connection."
  ([query-fn] (with-read-only query-fn {} {}))
  ([query-fn param-data] (with-read-only query-fn param-data {}))
  ([query-fn param-data opts]
   (with-open [conn (get-read-only-connection)]
     (query-fn conn param-data opts))))


; HugSQL query functions

(hugsql/declare-fn example-user--select :example-user/_)


#_(comment
    (with-read-only example-user--select))
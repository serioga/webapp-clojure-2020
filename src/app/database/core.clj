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

(mount/defstate ^Connection get-read-write-connection
  "Creates a connection to a database using read-write `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [ref'data-source (truss/have! future? (::ref'data-source-read-write (mount/args)))]
           (fn get-read-write-connection
             ([]
              (get-read-write-connection {}))
             ([options]
              (jdbc/get-connection @ref'data-source options)))))


(mount/defstate ^Connection get-read-only-connection
  "Creates a connection to a database using read-only `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [ref'data-source (truss/have! future? (::ref'data-source-read-only (mount/args)))]
           (fn get-read-only-connection
             ([]
              (get-read-only-connection {}))
             ([options]
              (jdbc/get-connection @ref'data-source options)))))


; Helper aliases for `with-open` with database connection

(defn with-read-write
  "Execute single HugSQL query with auto opened read-write connection."
  ([db-fn] (with-read-write db-fn {} {}))
  ([db-fn param-data] (with-read-write db-fn param-data {}))
  ([db-fn param-data opts]
   (with-open [conn (get-read-write-connection)]
     (db-fn conn param-data opts))))


(defn with-read-only
  "Execute single HugSQL query with auto opened read-only connection."
  ([db-fn] (with-read-only db-fn {} {}))
  ([db-fn param-data] (with-read-only db-fn param-data {}))
  ([db-fn param-data opts]
   (with-open [conn (get-read-only-connection)]
     (db-fn conn param-data opts))))


; HugSQL query functions

(hugsql/declare-fn example-user--select :example-user/_)


#_(comment
    (with-read-only example-user--select))
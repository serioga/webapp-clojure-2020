(ns app.database.core
  (:require [app.database.hugsql :as hugsql]
            [lib.clojure.core :as e]
            [mount.core :as mount]
            [next.jdbc :as jdbc])
  (:import (java.sql Connection)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
; Database connection

(mount/defstate ^Connection get-read-write-connection
  "Creates a connection to a database using read-write `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [ref'data-source (-> (::ref'data-source-read-write (mount/args))
                                   (e/assert future?))]
           (fn get-read-write-connection
             ([]
              (get-read-write-connection {}))
             ([options]
              (jdbc/get-connection @ref'data-source options)))))

(mount/defstate ^Connection get-read-only-connection
  "Creates a connection to a database using read-only `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [ref'data-source (-> (::ref'data-source-read-only (mount/args))
                                   (e/assert future?))]
           (fn get-read-only-connection
             ([]
              (get-read-only-connection {}))
             ([options]
              (jdbc/get-connection @ref'data-source options)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
; Helper aliases for `with-open` with database connection

(defn rw
  "Execute single HugSQL query with auto opened read-write connection.
   Prefer to pass `db-fn` as var `#'db-fn` to see query name on errors."
  ([db-fn] (rw db-fn {} {}))
  ([db-fn param-data] (rw db-fn param-data {}))
  ([db-fn param-data opts]
   (with-open [conn (get-read-write-connection)]
     (e/try-wrap-ex [[#'rw (-> db-fn meta :name)]
                     {:param-data param-data :opts opts}]
       (db-fn conn param-data opts)))))

(defn ro
  "Execute single HugSQL query with auto opened read-only connection.
   Prefer to pass `db-fn` as var `#'db-fn` to see query name on errors."
  ([db-fn] (ro db-fn {} {}))
  ([db-fn param-data] (ro db-fn param-data {}))
  ([db-fn param-data opts]
   (with-open [conn (get-read-only-connection)]
     (e/try-wrap-ex [[#'ro (-> db-fn meta :name)]
                     {:param-data param-data :opts opts}]
       (db-fn conn param-data opts)))))

(defmacro with-transaction
  "Helper for `jdbc/with-transaction` allowing specify connection
   symbol only once and shading non-transactable name."
  [spec & body]
  (let [spec (if (vector? spec)
               (into [(spec 0)] spec)
               [spec spec])]
    `(jdbc/with-transaction ~spec
                            ~@body)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
; HugSQL query functions

(hugsql/declare-fn example.list-user :example-user/_)

(comment
  (ro example.list-user))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

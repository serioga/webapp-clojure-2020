(ns app.database.core
  (:require [app.database.hugsql :as hugsql]
            [lib.clojure.core :as c]
            [mount.core :as mount]
            [next.jdbc :as jdbc])
  (:import (java.sql Connection)
           (javax.sql DataSource)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;;; Database connection

(mount/defstate ^Connection get-read-write-connection
  "Creates a connection to a database using read-write `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [data-source (::data-source-read-write (mount/args))]
           (c/assert-pred data-source (partial instance? DataSource) ::data-source-read-write)
           (fn get-read-write-connection
             ([]
              (get-read-write-connection {}))
             ([options]
              (jdbc/get-connection data-source options)))))

(mount/defstate ^Connection get-read-only-connection
  "Creates a connection to a database using read-only `data-source`."
  {:arglists '([] [options]) :on-reload :noop}

  :start (let [data-source (::data-source-read-only (mount/args))]
           (c/assert-pred data-source (partial instance? DataSource) ::data-source-read-only)
           (fn get-read-only-connection
             ([]
              (get-read-only-connection {}))
             ([options]
              (jdbc/get-connection data-source options)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;;; JDBC helpers

(defmacro with-transaction
  "Helper for `jdbc/with-transaction` allowing specify connection
   symbol only once and shading non-transactable name."
  [spec & body]
  (let [spec (if (vector? spec)
               (into [(spec 0)] spec)
               [spec spec])]
    `(jdbc/with-transaction ~spec
                            ~@body)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;;; HugSQL query functions

(hugsql/def example-user--select-all :example-user/_)

(comment
  (example-user--select-all))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

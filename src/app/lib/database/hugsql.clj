(ns app.lib.database.hugsql
  (:require
    [app.lib.util.exec :as exec]
    [clojure.java.io :as io]
    [hugsql.adapter.next-jdbc :as hugsql-adapter]
    [hugsql.core :as hugsql]))

(set! *warn-on-reflection* true)


(def def-db-fns-opts
  {:adapter (hugsql-adapter/hugsql-adapter-next-jdbc)})


(def sql-rc-path "app/database/sql/")


(defn sql-fn-body
  "Read SQL query from resource file for passing to `def-db-fns-from-string`.
  The name of HugSQL function is the same as file name."
  [name]
  (let [path (str sql-rc-path name ".sql")
        body (slurp (or
                      (io/resource path)
                      (exec/throw-ex-info "Missing SQL query file" path
                        {:name name :resource-path path})))]
    (str "-- :name " name "\r\n" body)))


(defmacro declare-fn
  "Declare single HugSQL function for symbol `sym`.
  The function definition string is loaded from resource file."
  [sym]
  `(hugsql/def-db-fns-from-string (sql-fn-body '~sym) def-db-fns-opts))

(ns app.database.hugsql
  (:require [app.database.hugsql-adapter :as hugsql-adapter]
            [app.database.result-set :as rs]
            [clojure.java.io :as io]
            [hugsql.core :as hugsql]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:const sql-rc-path
  "The resource path where loading .sql files are located."

  "app/database/sql/")

(defn def-db-fns-opts
  "Default opts of the HugSQL adapter."
  ([] (def-db-fns-opts rs/as-simple-maps))
  ([builder-fn]
   {:adapter (hugsql-adapter/hugsql-adapter-next-jdbc {:builder-fn builder-fn})}))

(defn db-fn-string
  "Read SQL query from resource file for passing to `def-db-fns-from-string`.
   The name of HugSQL function is the same as file name."
  [name]
  (let [path (str sql-rc-path name ".sql")
        body (slurp (or (io/resource path)
                        (throw (e/ex-info ["Missing SQL query file" path]
                                          {:name name :resource-path path}))))]
    (str "-- :name " name "\r\n" body)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro declare-fn
  "Declare single HugSQL function for symbol `sym`.
   The function definition string is loaded from resource file.

   If `namespace` (string or namespaced keyword) provided
   then all keys in result set are namespaced."
  ([sym]
   `(hugsql/def-db-fns-from-string (db-fn-string '~sym) (def-db-fns-opts)))
  ([sym, namespace]
   `(hugsql/def-db-fns-from-string (db-fn-string '~sym) (def-db-fns-opts (rs/as-namespaced-maps ~namespace)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

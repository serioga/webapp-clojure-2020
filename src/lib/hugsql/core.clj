(ns lib.hugsql.core
  (:require [hugsql.core :as hugsql]
            [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- sql-with-name
  "Adds function name in the body of SQL query as comment.
   For tracing SQL queries in logs."
  [s nom]
  (str "/* " nom " */\n" s))

(defn- name-parsed-def
  "Adds name to anonymously parsed def."
  [pdef nom]
  (-> pdef
      (update :hdr assoc :name [nom])
      (update-in [:sql 0] sql-with-name nom)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn intern-db-fn
  "Intern the db fn from a parsed def."
  [pdef options wrap-db-fn-map]
  (let [fm (cond-> (hugsql/db-fn-map pdef options) wrap-db-fn-map wrap-db-fn-map)
        fk (ffirst fm)]
    (intern *ns*
            (with-meta (symbol (name fk)) (-> fm fk :meta (assoc :sql (:sql pdef))))
            (-> fm fk :fn))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn def-db-fn-from-file
  "Interns the db fn for symbol `sym` from file in `path`."
  [sym path wrap-db-fn-map options]
  (-> (str path sym ".sql")
      (hugsql/parsed-defs-from-file)
      (first)
      (e/assert some? ["Parsed SQL with name" sym])
      (name-parsed-def sym)
      (intern-db-fn options wrap-db-fn-map)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

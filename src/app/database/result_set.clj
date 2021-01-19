(ns app.database.result-set
  (:require [lib.clojure.core :as e]
            [next.jdbc.optional :as jdbc.rs])
  (:import (java.sql ResultSet ResultSetMetaData)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- get-simple-column-names
  "Given `ResultSetMetaData`, return a vector of modified column names, each
  qualified by the `column-ns`."
  [^ResultSetMetaData rsmeta]
  (mapv (fn [^Integer i] (keyword (.getColumnLabel rsmeta i)))
        (range 1 (inc (.getColumnCount rsmeta)))))

(defn- get-namespaced-column-names
  "Given `ResultSetMetaData`, return a vector of modified column names, each
  qualified by the `column-ns`."
  [^ResultSetMetaData rsmeta, column-ns]
  (mapv (fn [^Integer i] (keyword column-ns (.getColumnLabel rsmeta i)))
        (range 1 (inc (.getColumnCount rsmeta)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn as-simple-maps
  "Given a `ResultSet` and options, return a `RowBuilder` / `ResultSetBuilder`
  that produces bare vectors of hash map rows, with simple keys and nil
  columns omitted."
  [^ResultSet rs _]
  (let [rsmeta (.getMetaData rs)
        cols (get-simple-column-names rsmeta)]
    (jdbc.rs/->MapResultSetOptionalBuilder rs rsmeta cols)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn as-namespaced-maps
  "Given a `ResultSet` and options, return a `RowBuilder` / `ResultSetBuilder`
  that produces bare vectors of hash map rows, with namespaced keys and nil
  columns omitted."
  [ns-tag]
  (let [column-ns (cond-> ns-tag
                    (ident? ns-tag) (namespace))]
    (e/assert column-ns (every-pred string? seq))
    (fn as-maps
      [^ResultSet rs _]
      (let [rsmeta (.getMetaData rs)
            cols (get-namespaced-column-names rsmeta column-ns)]
        (jdbc.rs/->MapResultSetOptionalBuilder rs rsmeta cols)))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

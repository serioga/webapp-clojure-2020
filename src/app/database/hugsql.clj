(ns app.database.hugsql
  (:require [app.database.result-set :as rs]
            [hugsql.adapter.next-jdbc :as adapter]
            [lib.clojure.core :as c]
            [lib.hugsql.core :as hugsql']
            [mount.core :as mount])
  (:import (javax.sql DataSource)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:const sql-rc-path
  "The resource path where loading .sql files are located."

  "app/database/sql/")

(defn def-db-fns-opts
  "Default opts of the HugSQL adapter."
  ([] (def-db-fns-opts rs/as-simple-maps))
  ([builder-fn]
   {:adapter (adapter/hugsql-adapter-next-jdbc {:builder-fn builder-fn})}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(mount/defstate ^:private data-source-read-write
  {:on-reload :noop}
  :start (-> (::data-source-read-write (mount/args))
             (doto (c/assert-pred (partial instance? DataSource) ::data-source-read-write))))

(mount/defstate ^:private data-source-read-only
  {:on-reload :noop}
  :start (-> (::data-source-read-only (mount/args))
             (doto (c/assert-pred (partial instance? DataSource) ::data-source-read-only))))

(defn- wrap-db-fn
  [f fn-name ds-var]
  (fn db-fn
    ([] (db-fn @ds-var {}))
    ([params] (db-fn @ds-var params))
    ([db params]
     (try (f db params)
          (catch Throwable e
            (throw (->> e (ex-info fn-name {:sql-params params}))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-db-fn-map
  "Wraps db fn with customized behaviour."
  [fm]
  (let [fk (ffirst fm)
        ds-var (if (-> fm fk :meta :command #{:query}) #'data-source-read-only
                                                       #'data-source-read-write)]
    (-> fm
        (update-in [fk :fn] wrap-db-fn (str "db/" (name fk)) ds-var)
        (update-in [fk :meta] assoc :arglists '([] [params] [db params])))))

(defmacro def
  "Defines database function with name `sym` in the current namespace.
   The function definition string is loaded from resource file.

   If `namespaced-as` (string or namespaced keyword) provided
   then all keys in result set are namespaced."
  ([sym]
   `(hugsql'/def-db-fn-from-file '~sym sql-rc-path wrap-db-fn-map (def-db-fns-opts)))
  ([sym, namespaced-as]
   `(hugsql'/def-db-fn-from-file '~sym sql-rc-path wrap-db-fn-map (def-db-fns-opts (rs/as-namespaced-maps ~namespaced-as)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

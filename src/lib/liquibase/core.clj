(ns lib.liquibase.core
  (:import (javax.sql DataSource)
           (liquibase.database Database DatabaseFactory)
           (liquibase.database.jvm JdbcConnection)
           (liquibase Liquibase Contexts)
           (liquibase.resource ClassLoaderResourceAccessor)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn update-database
  "Updates database schema using liquibase."
  [^DataSource ds, ^String changelog-path]

  (with-open [jdbc-conn (JdbcConnection. (.getConnection ds))]
    (let [db (-> (DatabaseFactory/getInstance)
                 (.findCorrectDatabaseImplementation jdbc-conn))
          contexts (Contexts.)
          resource-accessor (ClassLoaderResourceAccessor.)
          liquibase (Liquibase. changelog-path resource-accessor ^Database db)]
      (.update liquibase contexts))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

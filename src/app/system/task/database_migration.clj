(ns app.system.task.database-migration
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as e]
            [lib.liquibase.core :as liquibase]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.task/database-migration
  [_ {:keys [data-source changelog-path system-is-enabled] :as config}]
  (when system-is-enabled
    (e/future
      (logger/info (logger/get-logger *ns*) (e/p-str "Database migrations" config))
      (liquibase/update-database (e/unwrap-future data-source), changelog-path))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

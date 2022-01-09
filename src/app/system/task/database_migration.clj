(ns app.system.task.database-migration
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]
            [lib.liquibase.core :as liquibase]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.task/database-migration
  [_ {:keys [data-source, changelog-path, system-is-enabled] :as config}]
  (when system-is-enabled
    (logger/info (logger/get-logger *ns*) (c/pr-str* "Database migrations" config))
    (liquibase/update-database data-source, changelog-path))
  system-is-enabled)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

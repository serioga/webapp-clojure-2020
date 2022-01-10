(ns app.system.task.update-database-schema
  (:require [integrant.core :as ig]
            [lib.clojure-tools-logging.logger :as logger]
            [lib.clojure.core :as c]
            [lib.liquibase.core :as liquibase]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.task/update-database-schema
  [_ {:keys [data-source, changelog-path, system-is-enabled] :as config}]
  (when system-is-enabled
    (logger/info (logger/get-logger *ns*) (c/pr-str* "Update database schema" config))
    (liquibase/update-database data-source, changelog-path))
  system-is-enabled)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

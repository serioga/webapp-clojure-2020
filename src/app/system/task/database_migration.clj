(ns app.system.task.database-migration
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [lib.clojure.core :as e]
            [lib.liquibase.core :as liquibase]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.task/database-migration
  [_ {:keys [data-source changelog-path system-is-enabled] :as config}]
  (when system-is-enabled
    (e/future
      (log/info "Database migrations" (pr-str config))
      (liquibase/update-database (e/unwrap-future data-source), changelog-path))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

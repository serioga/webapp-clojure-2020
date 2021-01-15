(ns app.system.task.database-migration
  (:require
    [app.lib.database.liquibase :as liquibase]
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [lib.clojure.core :as e]))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod ig/init-key :app.system.task/ref'database-migration
  [_ {:keys [ref'data-source changelog-path enabled?] :as config}]
  (log/info "Database migrations" (pr-str config))
  (e/future (when enabled?
              (liquibase/update-database @ref'data-source, changelog-path))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

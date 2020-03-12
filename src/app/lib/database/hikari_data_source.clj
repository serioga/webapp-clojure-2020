(ns app.lib.database.hikari-data-source
  (:require
    [app.lib.util.secret :as secret]
    [clojure.spec.alpha :as s])
  (:import
    (com.zaxxer.hikari HikariDataSource)))

(set! *warn-on-reflection* true)


(s/check-asserts true)


(s/def ::data-source-class string?)
(s/def ::database-url string?)
(s/def ::database-user string?)
(s/def ::database-password ::secret/spec)
(s/def ::minimum-idle int?)
(s/def ::maximum-pool-size int?)
(s/def ::connection-timeout int?)
(s/def ::idle-timeout int?)
(s/def ::max-lifetime int?)
(s/def ::read-only? boolean?)
(s/def ::leak-detection-threshold int?)


(s/def ::options (s/keys :req-un [::data-source-class
                                  ::database-url
                                  ::database-user
                                  ::database-password]
                         :opt-un [::minimum-idle
                                  ::maximum-pool-size
                                  ::connection-timeout
                                  ::idle-timeout
                                  ::max-lifetime
                                  ::read-only?
                                  ::leak-detection-threshold]))


(defn- init-hikari-data-source
  "Force data source to connect after creation.
  Otherwise it's connected only during getting first connection."
  [^HikariDataSource ds]
  (.close (.getConnection ds))
  ds)


(defn create-data-source
  "Create HikariCP data source instance."
  [{:keys [data-source-class, database-url, database-user, database-password
           minimum-idle, maximum-pool-size, connection-timeout, idle-timeout, max-lifetime
           pool-name, read-only?, leak-detection-threshold]
    :or {read-only? false} :as options}]

  (s/assert ::options options)

  (doto (HikariDataSource.)
    (.setDataSourceClassName data-source-class)
    (.addDataSourceProperty "url" database-url)
    (.addDataSourceProperty "user" database-user)
    (.addDataSourceProperty "password" (secret/read-secret database-password))
    (cond->
      minimum-idle (doto (.setMinimumIdle minimum-idle))
      maximum-pool-size (doto (.setMaximumPoolSize maximum-pool-size))
      connection-timeout (doto (.setConnectionTimeout connection-timeout))
      idle-timeout (doto (.setIdleTimeout idle-timeout))
      max-lifetime (doto (.setMaxLifetime max-lifetime))
      leak-detection-threshold (doto (.setLeakDetectionThreshold leak-detection-threshold)))
    (.setReadOnly read-only?)
    (.setPoolName (str (when pool-name (str pool-name " "))
                       (if read-only? "RO" "RW")))
    (init-hikari-data-source)))

(ns app.system.integrant-config.hikari-data-source-mixin
  "Config template mixin for Hikari-CP data source.
  Example:
  {::data-source-read-write #::config{:mixins [::hikari-data-source-mixin]}
   ::data-source-read-only  #::config{:mixins [::hikari-data-source-mixin]
                                      :config {:read-only true}}}"
  (:require [app.system.integrant-config :as config]
            [clojure.test :as test]
            [integrant.core :as ig]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- hikari-data-source-mixin
  "Merges Hikari-CP data source options in the builder params."
  [_ {:builder/keys [params]}]
  (c/deep-merge params #::config{:derive :app.system.service/hikari-data-source
                                 :config {:dev-mode (ig/ref :app.system.core/dev-mode)}
                                 :import {:data-source-class "Database.DataSourceClassName"
                                          :database-url (if (-> params ::config/config :read-only)
                                                          "Database.Url.ReadOnly", "Database.Url")
                                          :database-user "Database.User"
                                          :database-password "Database.Password"}}))

(c/add-method config/builder-mixin :app.system.core/hikari-data-source-mixin hikari-data-source-mixin)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest hikari-data-source-mixin-test
  (test/are [arg ret] (= ret (config/build-config arg))
    #_arg {:test/read-write #::config{:mixins [:app.system.core/hikari-data-source-mixin], :mounts [:test/mount-rw]}
           :test/read-only, #::config{:mixins [:app.system.core/hikari-data-source-mixin], :mounts [:test/mount-ro], :config {:read-only true}}}
    #_ret {:app.system.service/mount {:test/mount-ro #integrant.core.Ref{:key :test/read-only}
                                      :test/mount-rw #integrant.core.Ref{:key :test/read-write}}
           [:app.system.service/hikari-data-source :test/read-only] #integrant.core.Ref{:key :test/read-only.config}
           [:app.system.service/hikari-data-source :test/read-write] #integrant.core.Ref{:key :test/read-write.config}
           [:lib.integrant.system/import-map :test/read-only.config] {:import-from #integrant.core.Ref{:key :app.system.service/app-config}
                                                                      :import-keys {:data-source-class "Database.DataSourceClassName"
                                                                                    :database-password "Database.Password"
                                                                                    :database-url "Database.Url.ReadOnly"
                                                                                    :database-user "Database.User"}
                                                                      :init-map {:dev-mode #integrant.core.Ref{:key :app.system.core/dev-mode}
                                                                                 :read-only true}}
           [:lib.integrant.system/import-map :test/read-write.config] {:import-from #integrant.core.Ref{:key :app.system.service/app-config}
                                                                       :import-keys {:data-source-class "Database.DataSourceClassName"
                                                                                     :database-password "Database.Password"
                                                                                     :database-url "Database.Url"
                                                                                     :database-user "Database.User"}
                                                                       :init-map {:dev-mode #integrant.core.Ref{:key :app.system.core/dev-mode}}}}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

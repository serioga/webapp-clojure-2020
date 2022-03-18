(ns app.system.integrant-config.app-config-setup
  "Config template setup for application properties.
  Example:
  {:app.system.service/app-config
    #::config{:setup ::app-config-setup
              :mounts [:lib.online-config.core/app-config]
              :config {:default-props \"app/config/default.props\"
                       :conform-rules {#\"System\\.Switch\\..+\" :edn
                                       #\"Webapp\\.Hosts\\(.+\\)\" :set
                                       #\".+\\.Password\" :secret
                                       #\".+\\.PublicKey\" :rsa-public-key
                                       #\".+\\.Secret\" :secret
                                       \"Mailer.Smtp.Port\" :edn
                                       \"Mailer.Smtp.Options\" :edn}}}}"
  (:require [app.system.integrant-config :as config]
            [clojure.test :as test]
            [integrant.core :as ig]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system/prepare-prop-files :lib.integrant.system/identity)

(defn- app-config-setup
  "Returns config map with addons:
  - Key :{config-key}.prop-files with \"config.file\" system property referred
    as :prop-files it the config value;
  - Key :dev.env.system/prepare-prop-files referred as :dev/prepare-prop-files
    in the config value."
  [{:builder/keys [config-map config-key params]}]
  (let [prop-files-key (config/suffix-key config-key ".prop-files")]
    (config/build-config config-map {:dev.env.system/prepare-prop-files nil
                                     prop-files-key #::config{:derive :lib.integrant.system/system-property
                                                              :config {:key "config.file"}}
                                     config-key (c/deep-merge params #::config{:config {:prop-files (ig/ref prop-files-key)
                                                                                        :dev/prepare-prop-files (ig/ref :dev.env.system/prepare-prop-files)}})})))

(c/add-method config/setup-builder :app.system.core/app-config-setup app-config-setup)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest app-config-setup-test
  (test/are [expr result] (= result expr)
    (config/build-config {:test/app-config #::config{:setup :app.system.core/app-config-setup
                                                     :mounts [:test/app-config-mount]
                                                     :config {:default-props "default.props"}}})
    #_=> {:dev.env.system/prepare-prop-files nil,
          [:lib.integrant.system/system-property :test/app-config.prop-files] {:key "config.file"},
          :app.system.service/mount #:test{:app-config-mount #integrant.core.Ref{:key :test/app-config}},
          :test/app-config {:prop-files #integrant.core.Ref{:key :test/app-config.prop-files},
                            :dev/prepare-prop-files #integrant.core.Ref{:key :dev.env.system/prepare-prop-files},
                            :default-props "default.props"}}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

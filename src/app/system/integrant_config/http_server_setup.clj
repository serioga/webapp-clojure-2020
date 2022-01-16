(ns app.system.integrant-config.http-server-setup
  "Config template setup for HTTP server.
  Example:
  {:app.system.service/immutant-web
    #::config{:setup ::http-server-setup
              :webapps {:app.system.service/homepage-http-handler
                         #::config{:config {:name \"homepage\"}
                                   :import {:hosts \"Webapp.Hosts(homepage)\"}}}
              :import {:options {:port \"HttpServer.Port\"}}
              :awaits [::ready-to-serve]}"
  (:require [app.system.integrant-config :as config]
            [clojure.test :as test]
            [integrant.core :as ig]
            [lib.clojure.core :as c]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :dev.env.system/prepare-webapp :lib.integrant.system/identity)

(defn- http-server-setup
  "Returns config map with addons:
  - Webapp keys from the ::config/webapps map, derived from
    :app.system.service/webapp-http-handler, referred as :webapps in the config
    value;
  - Key :dev.env.system/prepare-webapp referred as :dev/prepare-webapp it the
    config value."
  [{:builder/keys [config-map config-key params]}]
  (let [webapps (::config/webapps params)]
    (config/build-config config-map (into {:dev.env.system/prepare-webapp nil
                                           config-key (update params ::config/config merge
                                                              {:webapps (->> webapps (mapv (comp ig/ref first)))
                                                               :dev/prepare-webapp (ig/ref :dev.env.system/prepare-webapp)})}
                                          (map (fn [[k params]]
                                                 [k (c/deep-merge params #::config{:derive :app.system.service/webapp-http-handler
                                                                                   :config {:dev-mode (ig/ref :app.system.core/dev-mode)}})]))
                                          webapps))))

(c/add-method config/setup-builder :app.system.core/http-server-setup http-server-setup)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest http-server-setup-test
  (test/are [arg ret] (= ret (config/build-config arg))
    #_arg {:test/server #::config{:setup :app.system.core/http-server-setup
                                  :webapps {:app.system.service/homepage-http-handler #::config{:config {:name "homepage"} :import {:hosts "Webapp.Hosts(homepage)"}}
                                            :app.system.service/mobile-http-handler #::config{:config {:name "mobile"} :import {:hosts "Webapp.Hosts(mobile)"}}}
                                  :import {:options {:port "HttpServer.Port"}}
                                  :awaits [:test/ready-to-serve]}}
    #_ret {:dev.env.system/prepare-webapp nil,
           [:app.system.service/webapp-http-handler :app.system.service/homepage-http-handler] #integrant.core.Ref{:key :app.system.service/homepage-http-handler.config},
           [:lib.integrant.system/import-map :app.system.service/homepage-http-handler.config] {:init-map {:name "homepage",
                                                                                                           :dev-mode #integrant.core.Ref{:key :app.system.core/dev-mode}},
                                                                                                :import-from #integrant.core.Ref{:key :app.system.service/app-config},
                                                                                                :import-keys {:hosts "Webapp.Hosts(homepage)"}},
           [:app.system.service/webapp-http-handler :app.system.service/mobile-http-handler] #integrant.core.Ref{:key :app.system.service/mobile-http-handler.config},
           [:lib.integrant.system/import-map :app.system.service/mobile-http-handler.config] {:init-map {:name "mobile",
                                                                                                         :dev-mode #integrant.core.Ref{:key :app.system.core/dev-mode}},
                                                                                              :import-from #integrant.core.Ref{:key :app.system.service/app-config},
                                                                                              :import-keys {:hosts "Webapp.Hosts(mobile)"}},
           [:lib.integrant.system/import-map :test/server] {:import-from #integrant.core.Ref{:key :app.system.service/app-config}
                                                            :import-keys {:options {:port "HttpServer.Port"}}
                                                            :init-map {:app.system.integrant-config/await-refs {:test/ready-to-serve #integrant.core.Ref{:key :test/ready-to-serve}}
                                                                       :dev/prepare-webapp #integrant.core.Ref{:key :dev.env.system/prepare-webapp}
                                                                       :webapps [#integrant.core.Ref{:key :app.system.service/homepage-http-handler}
                                                                                 #integrant.core.Ref{:key :app.system.service/mobile-http-handler}]}}}))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

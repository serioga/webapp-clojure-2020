(defproject name.trofimov/webapp-clojure-2020 "1.0.0-SNAPSHOT"
  :description "Multi-page web application prototype with Clojure(Script)"
  :dependencies [; clojure
                 [org.clojure/clojure "1.10.1"]
                 ; system
                 [integrant "0.8.0"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.2"]
                 ; web server
                 [org.immutant/web "2.1.10"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 ; logging (clojure)
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]
                 [org.clojure/tools.logging "0.6.0"]
                 [org.codehaus.janino/janino "3.1.0"]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 ; libs
                 [clojurewerkz/propertied "1.3.0"]
                 [com.taoensso/truss "1.5.0"]
                 [org.apache.commons/commons-lang3 "3.9"]
                 ; daemon
                 [commons-daemon/commons-daemon "1.2.2"]]

  :main ^:skip-aot app.main
  :test-paths ["test" "src"]
  :target-path "target/%s"
  :plugins []

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/app"]

  :repl-options {:init-ns dev.main
                 :init (-main)}

  :profiles {:dev {:jvm-opts ["-Dconfig.file=dev-resources/dev/config/default.props"]
                   :main ^:skip-aot dev.main
                   :dependencies [[criterium "0.4.5"]
                                  [nrepl "0.6.0"]
                                  [ns-tracker "0.4.0"]
                                  [ring/ring-devel "1.8.0"]
                                  [zcaudate/hara.io.watch "2.8.7"]]
                   :source-paths ["dev"]}

             :test-release [:uberjar
                            {:jvm-opts ["-Dconfig.file=dev-resources/dev/config/default.props"]}]

             :uberjar {:aot :all}}

  :uberjar-name "website.jar")

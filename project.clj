(defproject name.trofimov/webapp-clojure-2020 "1.0.0-SNAPSHOT"
  :description "Multi-page web application prototype with Clojure(Script)"
  :dependencies [; clojure
                 [org.clojure/clojure "1.10.3"]
                 ; clojure script (shadow-cljs)
                 [com.google.guava/guava "30.1.1-jre" :scope "provided"]
                 [thheller/shadow-cljs "2.15.2" :scope "provided"]
                 ; system
                 [integrant "0.8.0"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.3"]
                 ; web server
                 [io.undertow/undertow-core,,,,,,,,,, "2.2.7.Final"]
                 [io.undertow/undertow-servlet,,,,,,, "2.2.7.Final"]
                 [io.undertow/undertow-websockets-jsr "2.2.7.Final"]
                 [metosin/reitit-core "0.5.13"]
                 [org.immutant/web "2.1.10"]
                 [ring/ring-core "1.9.3"]
                 [ring/ring-defaults "0.3.2"]
                 ; sql database
                 [com.h2database/h2 "1.4.200"]
                 [com.layerware/hugsql "0.5.1"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.1"]
                 [com.mattbertolini/liquibase-slf4j "4.0.0"]
                 [com.zaxxer/HikariCP "4.0.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.liquibase/liquibase-core "4.3.4"]
                 [seancorfield/next.jdbc "1.1.646"]
                 [p6spy/p6spy "3.9.1"]
                 ; logging (clojure)
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.codehaus.janino/janino "3.1.3"]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 [org.slf4j/slf4j-api "1.7.30"]
                 ; libs (clojure)
                 [clojurewerkz/propertied "1.3.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [commons-codec/commons-codec "1.15"]
                 [medley "1.3.0"]
                 [org.apache.commons/commons-lang3 "3.12.0"]
                 [potemkin "0.4.5"]
                 ; libs (clojure script)
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [rum "0.12.6"]
                 ; daemon
                 [commons-daemon/commons-daemon "1.2.4"]]

  :main ^:skip-aot app.main
  :test-paths ["test" "src"]
  :target-path "target/%s"
  :plugins [[lein-shell "0.5.0"]]

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/app"]

  :repl-options {:init-ns dev.env.main}

  :shell {:commands
          {"node_modules/.bin/postcss"
           {:windows "node_modules/.bin/postcss.cmd"}}}

  :aliases {"shadow-cljs" ["run" "-m" "shadow.cljs.devtools.cli"]

            "css-example" ["shell"
                           "node_modules/.bin/postcss"
                           "tailwind/app/_example_webapp_/main.css"
                           "-o" "resources/public/app/example/main.css"]}

  :profiles {:dev {:jvm-opts ["-Dconfig.file=dev-resources/app/config/default.props"]
                   :main ^:skip-aot dev.env.main
                   :dependencies [[compojure "1.6.2" #_"For ring-refresh"]
                                  [nrepl "0.8.3"]
                                  [ns-tracker "0.4.0"]
                                  [ring-refresh "0.1.2"]
                                  [ring/ring-devel "1.9.3"]
                                  [zcaudate/hara.io.watch "2.8.7"]]
                   :source-paths ["dev" "tailwind"]}

             :test-release [:uberjar
                            {:jvm-opts ["-Dconfig.file=dev-resources/app/config/default.props"]}]

             :uberjar {:aot :all
                       :prep-tasks ["compile"
                                    ["shadow-cljs" "release" "example"]
                                    "css-example"]}}

  :uberjar-name "website.jar")
